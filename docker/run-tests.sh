#!/usr/bin/env bash
# Container entrypoint: runs the API suite, the Onboarding UI suite, or both, with the project's own Maven/TestNG
# commands (nothing here changes how the tests run). Selected with TEST_TARGET (default: api, the original behaviour):
#   api  -> src/test/resources/testng/testng-api.xml
#   ui   -> src/test/resources/testng/testng-onboarding-ui.xml   (optionally narrowed with UI_GROUPS, e.g. "smoke")
#   all  -> api, then ui; both always run, even if the first fails.
#   budget -> src/test/resources/testng/testng-budget-ui.xml  (Budget Creation / Budget Alerts on the main QA app). Needs
#           BUDGET_UI_USERNAME / BUDGET_UI_PASSWORD. Opt-in only: it is NOT part of "all". BUDGET_GROUPS selects the TestNG
#           groups and defaults to "budget-readonly" (saves nothing). The write workflow, which creates and submits one
#           uniquely titled budget per run, must be requested explicitly: BUDGET_GROUPS=budget-write (or "budget" for all).
#
# Extra arguments given to the container (docker run <image> -Dfoo=bar ...) are appended to every Maven command, as
# they were when the entrypoint was the bare `mvn ... test` command. With TEST_TARGET=all they go to BOTH suites, so
# only pass options that make sense for both (e.g. -X); a group filter such as -Dgroups=smoke would also be applied
# to the API suite, which has no such group (the zero-test guard below then fails that suite). Use UI_GROUPS to
# narrow only the UI suite.
#
# After each suite the TestNG report is checked (docker/CheckReport.java): a suite fails even if Maven exited 0 when
# the report is missing, invalid or inconsistent, contains any failed test or configuration method, or shows no test
# EXECUTED (zero total, filtered out, everything skipped). So a typo in UI_GROUPS, an all-skipped run, or Maven being
# told to ignore failures (e.g. -Dmaven.test.failure.ignore=true) can never be a green build.
#
# Exit codes: 0 every selected suite passed; otherwise the exit code of the FIRST failing suite in run order
# (api, then ui): Maven's own exit code when Maven failed, or 4 when Maven succeeded but the report check failed.
# Setup errors before any test runs: 2 = unknown TEST_TARGET, 3 = missing UI credentials.
#
# Credentials are read from the environment by the framework (API_USERNAME/API_PASSWORD,
# ONBOARDING_UI_USERNAME/ONBOARDING_UI_PASSWORD, BUDGET_UI_USERNAME/BUDGET_UI_PASSWORD); this script never prints their values.
set -u

TEST_TARGET="${TEST_TARGET:-api}"
API_SUITE="src/test/resources/testng/testng-api.xml"
UI_SUITE="src/test/resources/testng/testng-onboarding-ui.xml"
BUDGET_SUITE="src/test/resources/testng/testng-budget-ui.xml"
REPORTS_COPY_DIR="${REPORTS_COPY_DIR:-/app/test-reports}"   # per-suite copies; target/ is wiped by each `mvn clean`
REPORT_XML="${REPORT_XML:-target/surefire-reports/testng-results.xml}"
CHECK_REPORT="${CHECK_REPORT:-/usr/local/lib/brandrunner/CheckReport.java}"
EXTRA_ARGS=("$@")

case "$TEST_TARGET" in
  api|ui|budget|all) ;;
  *) echo "Unknown TEST_TARGET '$TEST_TARGET' (expected: api, ui, budget or all)" >&2; exit 2 ;;
esac

# True when the variable is unset, empty or only whitespace.
is_blank() {
  local value="${1:-}"
  [ -z "${value//[[:space:]]/}" ]
}

if [ "$TEST_TARGET" = "ui" ] || [ "$TEST_TARGET" = "all" ]; then
  # Without these the UI tests would fail or skip instead of testing the portal; reject it up front, with no values shown.
  missing=""
  is_blank "${ONBOARDING_UI_USERNAME:-}" && missing="ONBOARDING_UI_USERNAME"
  is_blank "${ONBOARDING_UI_PASSWORD:-}" && missing="${missing:+$missing and }ONBOARDING_UI_PASSWORD"
  if [ -n "$missing" ]; then
    echo "TEST_TARGET=$TEST_TARGET needs a non-blank $missing in the environment." >&2
    exit 3
  fi
fi
if [ "$TEST_TARGET" = "budget" ]; then
  missing=""
  is_blank "${BUDGET_UI_USERNAME:-}" && missing="BUDGET_UI_USERNAME"
  is_blank "${BUDGET_UI_PASSWORD:-}" && missing="${missing:+$missing and }BUDGET_UI_PASSWORD"
  if [ -n "$missing" ]; then
    echo "TEST_TARGET=budget needs a non-blank $missing in the environment." >&2
    exit 3
  fi
fi

# Copies this suite's reports to $REPORTS_COPY_DIR/<name>. Only THIS suite's destination is replaced (never the other
# suite's), so a reused directory cannot keep stale files from an earlier run. A failure here is only a warning: it
# never changes the suite's result (run_suite's return value is decided from Maven's exit code and the report check).
save_reports() {   # save_reports <api|ui>
  local name="$1" source_dir dest
  source_dir="$(dirname "$REPORT_XML")"
  dest="$REPORTS_COPY_DIR/$name"
  if ! rm -rf "$dest" || ! mkdir -p "$dest"; then
    echo "WARNING: could not prepare $dest; the $name reports were not saved" >&2
    return 0
  fi
  if [ ! -d "$source_dir" ]; then
    echo "WARNING: no report directory ($source_dir) was produced by the $name suite; nothing was saved to $dest" >&2
    return 0
  fi
  if ! cp -r "$source_dir/." "$dest/"; then
    echo "WARNING: copying the $name reports to $dest failed; the saved reports may be incomplete" >&2
  fi
  return 0
}

run_suite() {   # run_suite <name> <suite xml> [groups]; returns the suite's exit code
  local name="$1" suite="$2" groups="${3:-}" mvn_rc check_rc summary
  local args=(-B -ntp clean test "-DsuiteXmlFile=$suite")
  if [ -n "$groups" ]; then args+=("-Dgroups=$groups"); fi
  echo "=== Running the $name suite: $suite${groups:+ (groups: $groups)}"
  mvn "${args[@]}" ${EXTRA_ARGS[@]+"${EXTRA_ARGS[@]}"}
  mvn_rc=$?

  save_reports "$name"

  summary="$(java "$CHECK_REPORT" "$REPORT_XML" 2>&1)"
  check_rc=$?
  echo "=== $name suite report: $summary"

  if [ "$mvn_rc" -ne 0 ]; then
    echo "=== $name suite FAILED: Maven exit code $mvn_rc"
    return "$mvn_rc"
  fi
  if [ "$check_rc" -ne 0 ]; then
    echo "=== $name suite FAILED: Maven succeeded but the TestNG report check failed (see the report line above)" >&2
    return 4
  fi
  echo "=== $name suite passed"
  return 0
}

final=0
note_result() {   # keeps the first failing suite's exit code
  if [ "$final" -eq 0 ]; then final="$1"; fi
}

if [ "$TEST_TARGET" = "api" ] || [ "$TEST_TARGET" = "all" ]; then
  run_suite api "$API_SUITE"
  note_result $?
fi
if [ "$TEST_TARGET" = "ui" ] || [ "$TEST_TARGET" = "all" ]; then
  run_suite ui "$UI_SUITE" "${UI_GROUPS:-}"
  note_result $?
fi
if [ "$TEST_TARGET" = "budget" ]; then
  # Read-only by default; the write workflow needs BUDGET_GROUPS=budget-write (or budget) to be asked for explicitly.
  run_suite budget "$BUDGET_SUITE" "${BUDGET_GROUPS:-budget-readonly}"
  note_result $?
fi
exit "$final"
