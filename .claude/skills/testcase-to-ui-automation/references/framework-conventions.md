# BrandRunner_E2E_Test conventions (as used for Onboarding and Budget)

Stack: Java 21, Maven, Selenium 4.42, TestNG 7.11, Allure, Lombok, Log4j. The Budget module is the reference implementation of everything below; copy its shape for a new module.

## Layers and where files go

| Layer | Path (Budget example) | Role |
|---|---|---|
| Wrappers | `src/main/java/ai/metayb/ui/wrappers/BaseDriver.java` | `startApp`, thread-local `getDriver()`, `locateElement`, `click`, `type`/`typeWithoutClear`/`replaceText`/`clearField`, `typeSecret` (never logs the value), `getText`, `verify*`, `reportStep` (a FAIL attaches a screenshot and throws `AssertionError`), `isPresent`, `isDisplayed`. Reuse these; do not call raw Selenium for clicks and typing. |
| Locators | `src/main/java/ai/metayb/ui/components/budget/BudgetElements.java` | XPath constants in nested classes per page area (`Nav`, `ListPage`, `Create`, `Picker`, `Drawer`, `Popup`) plus helpers (`lit()` quotes any string for XPath, `caption()`, `captionContaining()`, `amountAfter()`). Login locators in their own class. |
| Page base | `.../ui/pages/budget/BudgetPageBase.java` | quiet queries (`visible`, `exists`, `count`, `domTextOf`, `textsOf`), non-failing `waitUntil(BooleanSupplier, seconds)`, MUI select helpers, `parseAmount` (strips currency symbol and commas). |
| Pages | `BudgetLoginPage`, `BudgetNavigationPage`, `BudgetListPage`, `BudgetCreatePage`, `BudgetActivityDrawer`, `BudgetDashboardPage` | one class per page or dialog; WRITE methods are few, named plainly, and documented as WRITE. |
| Test support | `src/test/java/testUtils/BudgetBaseTest.java` (browser setup from config), `BudgetAuthenticatedTest.java` (login only if the form is shown, `SkipException` when credentials are missing, select the business unit) | `BaseTest` provides `logger`; `ConvertBrokenToFailedListener` turns errors into failures. |
| Test data | `src/test/java/ai/metayb/ui/budget/BudgetTestData.java` | constants from the document's "common test data" section, `uniqueTitle(testId)`, month helpers. No credentials. |
| Records | `BudgetRecords.java` | `track(...)` for the serial chain, `trackStandalone(...)` for a one-off test; logs, Allure attachment, `target/budget-created-records.txt`; never deletes. |
| Form base | `BudgetFormTestBase.java` | read-only tests: open the form, and an `@AfterMethod(alwaysRun = true)` that closes dialogs and cancels without failing. |
| Tests | `src/test/java/ai/metayb/ui/budget/*Test.java` | one class per feature area; one serial write chain class (`BudgetDraftAndSubmitWorkflowTest`); one class of blocked/skipped cases (`BudgetBlockedScenariosTest`, DataProvider for catalogues such as error toasts). |
| Suite | `src/test/resources/testng/testng-budget-ui.xml` | package-based (`ai.metayb.ui.budget.*`) with `AllureTestNg` and `ConvertBrokenToFailedListener` listeners; new tests in the package are picked up without editing it. Header comment explains groups and commands. |
| Config | `src/main/java/ai/metayb/config/ConfigManager.java` | precedence env var -> system property -> `config/config.properties` -> legacy value. Budget keys: `BUDGET_UI_URL` (default `ui.url`), `BUDGET_UI_BROWSER`, `BUDGET_UI_HEADLESS`, `BUDGET_UI_USERNAME`, `BUDGET_UI_PASSWORD` (env only). New modules get their own `<MODULE>_UI_*` block, same shape. |
| Docker | `docker/run-tests.sh`, `docker/CheckReport.java` | `TEST_TARGET` = api / ui / budget / all. A UI target needs non-blank credentials (exit 3 otherwise), runs `mvn ... -DsuiteXmlFile=<xml> -Dgroups=<groups>`, then `CheckReport` fails the run on any failure, zero executed tests, or inconsistent counts (exit 4). The module's default group is read-only; the write group must be requested explicitly (`BUDGET_GROUPS=budget-write`). Not part of `all`. |

## Groups (TestNG)

`<module>` every test · `<module>-readonly` saves nothing, default regression · `<module>-write` creates records, opt-in, serial · `<module>-alerts` message/alert checks · `<module>-defect` asserts the expected behaviour of a known defect, fails while it exists · `<module>-smoke` shortest check · `<module>-slow` long tests (for example the 25-item limit) · `<module>-blocked` documented but not automatable, reported as skipped with the reason.

A test that is read-only AND a defect check carries both groups, so the default read-only run includes it.

## Serial write chain

```java
@Test(groups = {"budget", "budget-write"}) public void e2e02Create...() { ... }
@Test(groups = {"budget", "budget-write"}, dependsOnMethods = "e2e02Create...", alwaysRun = true) public void e2e03...() { ... }
```
`alwaysRun = true` keeps later steps running when an earlier step fails on a known defect, so one defect does not hide the rest. Each later step starts with a hard precondition (the record from the previous step exists) so a failure is attributed correctly. Steps share state through a small static holder (title, ids), never through the page.

## Assertions and messages

- `SoftAssert` for multi-rule tests, `soft.assertAll()` last; hard `Assert` for preconditions.
- Message pattern: `<defect id> / <test case id> <what should happen> but <what happened>`, including the actual values. A failure should be understandable from the message alone.
- Messages quoted from the document go in verbatim (including the Unicode dash in "Days must be 1–30"; match a stable prefix with `contains` when the number varies, as the page does).
- Money: compare `BigDecimal` with `compareTo`, parse through `parseAmount`.

## Test data and uniqueness

Title `QA-Claude-<BU>-<testId>-<yyyyMMddHHmmss>`. Use next month (current month is the minimum the app accepts). Use the document's fixed hierarchy, product, designation and rates. Do not modify shared QA budgets, business-unit configuration or integrations.

## Allure

Class: `@Epic`, `@Feature`. Method: `@Story`, `@Severity`, `@Description` (state clearly if the test WRITES, and which defect it asserts). Attach ledgers/records with `Allure.addAttachment(name, "text/plain", text)`.

## Running

```
mvn -B -ntp clean test -DsuiteXmlFile=src/test/resources/testng/testng-<module>-ui.xml -Dgroups=<module>-readonly
mvn -B -ntp clean test -DsuiteXmlFile=... -Dtest=MyTest#myMethod*        # targeted
```
Environment for a local stand-in: `<MODULE>_UI_URL=http://localhost:<port>/ <MODULE>_UI_USERNAME=... <MODULE>_UI_PASSWORD=... UI_HEADLESS=true`. Test results: `target/surefire-reports/testng-results.xml` (parse it to count PASS/FAIL/SKIP and list failure messages; write output with `PYTHONIOENCODING=utf-8` on Windows because of the naira sign and dashes).
