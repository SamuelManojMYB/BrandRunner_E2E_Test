---
name: testcase-to-ui-automation
description: Convert a manual test-case document (docx, xlsx, csv, pasted table) into Java/Maven/Selenium/TestNG UI automation inside the BrandRunner_E2E_Test framework, with page objects, locators, every documented validation and alert message, known-defect assertions, safe write handling, a suite file, Docker support and a verified pass/fail report. Use this whenever the user hands over test cases, a test-steps document, a TC/BC/AL/E2E id list, a regression scenario or a defect id (D-xx) and wants it automated, stabilised or added to a UI suite, even if they only say "automate this", "write the script for these cases", "add this scenario to the regression" or "here are the test cases".
---

# Test cases -> UI automation (BrandRunner_E2E_Test)

You turn a test-case document into working Selenium/TestNG tests that follow this repository's conventions. The document is the source of truth: every expected result, message and known defect in it must end up as an assertion, a documented skip, or a flagged question. Nothing is silently dropped and nothing is invented.

Read `references/framework-conventions.md` before writing code (layers, wrappers, groups, config, Docker). Read `references/pitfalls.md` before writing locators or waits (it lists the mistakes that cost the most time on earlier modules). `references/templates.md` has skeletons.

## Safety rules (they override everything below)

- Never type real credentials into a real system yourself. Authenticated tests read `<MODULE>_UI_USERNAME` / `<MODULE>_UI_PASSWORD` from the environment through `ConfigManager`; the user runs them against QA. Without credentials the tests skip, they do not fail. Never print, log or hard-code secrets or tokens; do not repeat them in chat.
- Tests that save, submit or create anything write backend records. Use unique titles (`QA-Claude-<BU>-<testId>-<timestamp>`), track every created id (log, Allure, `target/...-created-records.txt`), run write tests serially, and never delete or edit shared or pre-existing data. Do not add deletion unless the user asks for it (even "safe cleanup" of your own record needs their yes); instead report the created record and the cleanup limitation.
- Do not commit or push unless asked. Do not modify `pom.xml`, existing buildspecs, existing suites or completed modules; touch the generic framework only when a real gap is proven.
- Do not weaken an assertion so a test goes green. A known defect stays a visible failure.

## Workflow

### 1. Read the document completely
Run the extractor (outputs to a scratch directory, UTF-8):
`python -I .claude/skills/testcase-to-ui-automation/scripts/extract_testcases.py <doc.docx> <scratch_dir>`
Then read `summary.txt` and `document.txt`; grep `catalogue.tsv` by id. For xlsx/csv read the sheet and build the same view. Collect:
- common test data (business unit, hierarchy, products, rates, accounts by role, unique-title pattern, month/date rules);
- preconditions shared by groups of cases (the document often has tags like [PRE-LOC]); the alert/message catalogue (exact wording); the known-defect list.
If the user passed only ids or a scenario description, locate those rows and read their neighbours too, because related rules (limits, formulas, "confirm" notes) usually live next to them.

### 2. Inspect the framework before changing it
Look at the existing module closest to this one: page-object base, element-constant classes, base test classes, `ConfigManager`, suite XMLs, `docker/run-tests.sh`, and how groups are named. Reuse the wrappers in `BaseDriver` and the existing locator style. Check what already covers the requested ids (grep the ids and the defect ids in `src/test`) so you extend instead of duplicate. Write down which existing tests must keep passing.

### 3. Classify every test case before coding
Build a table (kept in your notes or the final report): id, title, class (read-only / write / blocked), group, defect id if any, expected result, planned assertion.
- **Read-only**: reaches and checks UI state without saving. Preferred; run in the default regression.
- **Write**: needs Done/Save/Submit or otherwise creates a record. One serial chain per module (`dependsOnMethods` + `alwaysRun = true` so a failing step does not hide the rest), unique titles, tracked ids.
- **Blocked**: needs something automation cannot do safely or deterministically (a second account, native date-picker popups, network faults, file upload, transient loaders, "Not Run" with no data, a requirement marked "confirm"/"define"). Implement as a `SkipException` with the reason and the document id, in a `*-blocked` group, so it shows as skipped, not missing.
- **Ambiguous requirement** (the document says "confirm", gives no message wording, or two cases conflict): assert only what is unambiguous, record the rest as an observation (log/Allure ledger), and list the open question for the user. Do not guess a formula or a message.

### 4. Verify the live DOM before writing locators (when authorised)
If a signed-in browser session is available to you, evaluate XPath against the live pages read-only (never press save/submit, never type credentials). If it is not, say so and mark locators as "derived from the document, not live-verified". Details and the traps (hashed CSS classes, MUI selects, CSS-uppercased captions, async relabelling) are in `references/pitfalls.md`.

### 5. Implement in the existing layers
- Locators: a `<Module>Elements` class with nested classes per page area and small XPath helpers. Prefer visible text, `role`, `aria-label`, `type`, `name`; never position-only or coordinates; never rely on hashed class names.
- Page objects: one base with quiet queries and non-failing `waitUntil`, then one class per page/dialog. Read-only and WRITE methods are clearly separated, and the WRITE ones say so in Javadoc (`clickDone()`, `saveAsDraft()`, `submit()`).
- Tests: one class per feature area; test names carry the document ids and defect ids (`bc062D48DecimalDaysRejected`); `@Epic/@Feature/@Story/@Severity/@Description` for Allure; groups `<module>`, `<module>-readonly`, `<module>-write`, `-alerts`, `-defect`, `-smoke`, `-slow`, `-blocked`.
- Assertions: use `SoftAssert` when one test checks several rules so every failure shows; keep preconditions as hard `Assert` so a negative check is never vacuous; include the valid-value control case next to every negative case (it proves the field works and the test can pass); assert exact documented message text where the document gives it, and where it gives none assert the behaviour (blocked/disabled + some feedback) and say that the wording is unspecified.
- Known defects: assert the documented EXPECTED behaviour and let the test fail with the defect id, actual and expected in the message. No retry, no `expectedExceptions`, no try/catch swallowing.
- Waits: state-based (`WebDriverWait`/`waitUntil`), never `Thread.sleep`, and no `@Test(timeOut)` (it runs the test on another thread and the thread-local driver becomes null).
- Numeric states (totals, counters): read through a small snapshot object and log each state in a "ledger" attached to Allure, so the actual figures are visible even when the assertion fails.
- Leave pages safely: an `@AfterMethod(alwaysRun = true)` closes open dialogs and cancels, and never fails the test itself.
- Config: new settings go through `ConfigManager` (env var, then system property, then config file). Add the suite XML for the module (package-based so new tests are picked up automatically) and a `docker/run-tests.sh` target if the module is new, with the write group opt-in.

### 6. Validate without real credentials
You usually cannot sign in to QA, so prove the framework logic against a local stand-in and be honest about what that proves:
1. Build a small single-page stand-in (scratch directory, `node server.js <port> [page.html]`) that mimics the real DOM conventions (roles, aria-labels, text) and reproduces the documented defects.
2. Run the read-only group, then the write group, then the targeted new tests.
3. Make a second copy of the stand-in with the defects fixed and run the new defect tests again: they must pass there. That is the check that the assertions discriminate, not just fail.
4. Classify every failure as (a) documented defect assertion, (b) automation fault, (c) stand-in artefact. Fix (b) and (c); never "fix" (a).
5. Re-run the whole module suite and confirm earlier modules still compile and their files are unchanged (`git status` on `pom.xml`, buildspecs, Dockerfile, other modules' tests).
Stand-in results are not evidence of live behaviour; say so in the report. Restart the stand-in server after every page patch (it reads the file at start). Stop it at the end.

### 7. Hygiene before reporting
Compile everything; secret-scan the changed files (no passwords, tokens, real account names); confirm no stray files in the repo (scratch lives in the scratchpad directory); check the diff touches only the intended files. For the Docker target, check the exit-code matrix with a stubbed `mvn`/`java` (blank credentials -> 3, unknown target -> 2, Maven failure passthrough, report-check failure -> 4).

### 8. Report (concise)
1. Tests implemented (ids covered, groups) and where they are included in the regression run.
2. Tests executed: exact passed / failed / skipped counts, split into documented-defect failures vs automation failures, and stated as stand-in only unless run on QA.
3. Blockers: credentials, unverified locators, ambiguous requirements and the exact question to resolve each.
4. Known defects asserted (id, actual vs expected as documented).
5. Records created and not cleaned up (title, id) with the cleanup limitation.
6. Files changed.
No extra documentation files unless the user asks. Say "nothing committed" when that is the case.

## When the user gives only part of the picture
- Only a defect id or one scenario ("D-48", "totals include unchecked items"): do steps 1-8 scoped to that, reading the neighbouring rules so the expected behaviour is exact. Look up the id in the document first; if the document marks the rule "confirm", build the test on the documented expectation and flag the confirmation.
- A document for a module that does not exist yet: create the module layers by copying the shape of the nearest module (framework-conventions.md), then proceed.
- The user later says "run it on QA": ask them to run the documented command with their credentials in the environment, or run it yourself only if they have already set the variables; then re-classify any new failures (automation vs defect) before reporting.
