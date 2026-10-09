# Onboarding Portal UI Automation: Final Reconciliation (Module 14)

Date of reconciliation: 2026-10-09. Portal under test: `https://qas-onboarding.brandrunners.ai`.
Suite: `src/test/resources/testng/testng-onboarding-ui.xml` (package `ai.metayb.ui.onboarding.*`).

This is a new document. It does not replace any earlier report.

## 1. Final status

**Complete with documented skips.**

- All onboarding modules and the read-only Critical E2E tests are implemented and compile.
- E2E-002 and E2E-003 are skipped by scope decision (section 5).
- Real-portal evidence is the project owner's own runs, reported in conversation; it was not re-run in this reconciliation (section 3).

## 2. Coverage and current results

Counts come from the test sources and from the TestNG result file of the run described in section 6. "Stand-in" means the local mock portal (section 3), **not** the real portal.

| # | Module | Test class | Group(s) | Tests | Stand-in result (2026-10-09) |
|---|---|---|---|---|---|
| 1 | Login | `LoginTest` | `regression` (1 test also `smoke`) | 6 | 6/6 |
| 2 | Tenant Dashboard | `DashboardTest` | `dashboard`, `regression` (1 test also `smoke`) | 13 | 13/13 |
| 3 | Business Unit Details | `BusinessUnitDetailsTest` | `businessunit` | 15 | 15/15 |
| 4 | Currency | `CurrencySectionTest` | `currency` | 15 | 15/15 |
| 5 | ERP Integration | `ErpIntegrationSectionTest` | `erp` | 14 | 14/14 |
| 6 | Payment Integration | `PaymentIntegrationSectionTest` | `payment` | 15 | 15/15 |
| 7 | CDP Integration | `CdpIntegrationSectionTest` | `cdp` | 14 | 14/14 |
| 8 | OTP Verification | `OtpVerificationSectionTest` | `otp` | 9 | 9/9 |
| 9 | Geofence | `GeofenceSectionTest` | `geofence` | 15 | 15/15 |
| 10 | Password Management | `PasswordManagementTest`, `PasswordUnauthenticatedAccessTest`, `SecretMaskingTest` | `password` (the last two also `password-nologin`) | 8 + 1 + 5 = 14 | 14/14 |
| 11 | Tenant Crons | `TenantCronsTest` | `crons` | 7 | 7/7 |
| 12a | Create Business Unit, Activity Communication | `ActivityCommunicationSectionTest` | `activity` | 12 (ACT-001..012) | 12/12 |
| 12b | Create Business Unit, dialog level | `CreateBusinessUnitDialogTest` | `createbu` | 10 (CBU-001..010) | 10/10 |
| 13 | Critical E2E, read-only | `CriticalE2eReadOnlyTest` | `critical-e2e` only | 2 (E2E-001, E2E-004) | 2/2 |
| | **Total** | | | **161** | |

- Group `regression` covers every class above except `critical-e2e`: **159 tests, 159 passed** on the stand-in.
- Group `smoke`: **2 tests, 2 passed** on the stand-in.
- Group `critical-e2e`: **2 tests, 2 passed** on the stand-in.
- `critical-e2e` is deliberately not part of `regression`, so a regression run on the shared tenant never pulls them in.

## 3. Evidence quality

| Evidence | Status |
|---|---|
| E2E-001 and E2E-004 passed on the real QA portal | **Reported by the project owner.** Not re-run in this reconciliation. |
| Modules 1 to 12 stable on the real portal | **Reported by the project owner** at the end of each module (the owner's own runs). Not re-run in this reconciliation. |
| Stand-in results in section 2 | **Local stand-in only.** Not real-portal validation. |
| Real-portal re-run during reconciliation | **Not run.** Running the suite on the real portal requires signing in with the portal account, which the assistant did not do. |

The stand-in is a small local web page (`mock/page.html` and `server.js`, port 8765), kept **outside the repository** in a session scratchpad. It mimics the portal's DOM structure from the live inspections. Consequences:

- It cannot catch real-portal behaviour differences or application defects.
- It is not reproducible by other team members from the repository alone.
- Its Edit Details dialog and Login page were extended during this work purely so the existing tests could be exercised.

## 4. Failures found in this reconciliation

- The first full regression run on the stand-in showed **5 failures, all in `LoginTest`**. Cause: the stand-in's login page had no validation messages, forgot-password link or password eye button, so the existing locators found nothing. The Login tests had never been run against the stand-in before.
- **Not an automation defect and not a portal defect.** No repository file was changed. The stand-in's login page was extended to model what `LoginPageElements` expects; `LoginTest` then passed 6/6 and the whole regression passed 159/159.
- Earlier in the session, a run was interrupted when the working session ended. That was an environment interruption, not a test failure, and the run was repeated in full.

## 5. Skipped by scope decision: write-operation safety

| Test | Status | Reason |
|---|---|---|
| E2E-002: change one safe setting, Update, reopen, verify, restore | **Skipped by scope decision, write-operation safety** | Requires clicking Update. Rollback is unverified. |
| E2E-003: change several settings, Update, verify, restore all | **Skipped by scope decision, write-operation safety** | Same, with more fields. |

Evidence behind the decision (from the inspection of Edit Details; nothing was executed):

- Update is `POST /business-unit/settings/{tenantId}/{businessUnitId}` (form data) and always sends the **full settings array of all seven types**, including untouched and inactive ones.
- Server-side effects are unknown: whether untouched types get written, whether any external call or background job starts, whether history or audit rows are kept.
- The UI offers no revert, delete or archive for settings, so setting a field back to its original value does not prove rollback.
- Neither test has been implemented or run. They do not block completion.

## 6. Commands used (exact)

Compile:

```bash
mvn -B -ntp clean test -DskipTests
```

Critical E2E read-only group:

```bash
mvn clean test "-DsuiteXmlFile=src/test/resources/testng/testng-onboarding-ui.xml" "-Dgroups=critical-e2e"
```

Full non-destructive onboarding regression (all groups in section 2 except `critical-e2e`) and smoke:

```bash
mvn clean test "-DsuiteXmlFile=src/test/resources/testng/testng-onboarding-ui.xml" "-Dgroups=regression"
mvn clean test "-DsuiteXmlFile=src/test/resources/testng/testng-onboarding-ui.xml" "-Dgroups=smoke"
```

Single module (replace the group name):

```bash
mvn clean test "-DsuiteXmlFile=src/test/resources/testng/testng-onboarding-ui.xml" "-Dgroups=createbu"
```

The group for the Business Unit Details module is `businessunit` (not `business-unit`).

Environment used for the stand-in runs: `ONBOARDING_UI_URL` pointed at the local stand-in with mock placeholder credentials, and `UI_HEADLESS=true`. For the real portal, supply the onboarding account through `ONBOARDING_UI_USERNAME` / `ONBOARDING_UI_PASSWORD`; no credential appears in this document.

Note: `pom.xml`'s default suite is `testng-api.xml`, so `-DsuiteXmlFile` must always be passed for onboarding runs.

## 7. Safety confirmation for the reconciliation runs

- No Create or Update was clicked, and neither is reachable from the Edit Business Unit page object (it has no Update or Create method).
- No setting, switch, status or logo was changed, and no integration was activated.
- No file was uploaded; no cron was added or removed.
- TATA and every other Business Unit were not touched.
- The reconciliation runs were on the stand-in only, where an Update or Create would have been caught by a safety net that never triggered.
- `SensitiveData` masking was not changed.

## 8. Repository changes

Made for the Critical E2E phase (new, untracked at the time of writing):

- `src/main/java/ai/metayb/ui/components/onboarding/EditBusinessUnitElements.java`
- `src/main/java/ai/metayb/ui/pages/onboarding/EditBusinessUnitDialog.java`
- `src/test/java/ai/metayb/ui/onboarding/criticale2e/CriticalE2eReadOnlyTest.java`

Made for this reconciliation:

- `docs/ONBOARDING_UI_FINAL_RECONCILIATION.md` (this file).

No existing source, test, configuration or suite file was modified in this phase.

## 9. Remaining gaps and known limitations

- **Real-portal evidence is the owner's.** A fresh real-portal run of `critical-e2e`, `regression` and `smoke` should be recorded by the owner when this document is accepted.
- **Stand-in not in the repository.** It lives outside the repo and is not reproducible; see section 3.
- **Baseline drift:** E2E-001 and E2E-004 assert the current state of "QA Test BU 01" and the tenant counts (2 Active, 0 Inactive). A legitimate change to the tenant makes them fail with a safe diagnostic; nothing is auto-corrected.
- **Observed, not endorsed, behaviour:** CBU-003/004 assert what the portal does today (Escape and an outside click do not close the dialog), and the portal can show "Unsaved changes" before any edit.
- **Create Business Unit success path** is not covered: creation cannot be undone and no disposable tenant exists. Only the safe scope (dialog, sections, validation, persistence between sections) is covered.
- **Edit Business Unit writes** are not covered (E2E-002/003, section 5).
- **Stale comment** in `testng-onboarding-ui.xml` says only `LoginTest` is covered; the suite itself picks up every class by package. Not changed (out of scope).
- **Timing:** negative checks use a fixed 3 s window, and the portal's Edit settings load can be slow (the wait allows 30 s).
- **Browser:** runs were Chrome (headless on the stand-in); other browsers are not verified.
