# Skeletons

Adapt names to the module (`Budget` below). Keep the existing style: Allure annotations, groups, ids in names.

## Read-only negative test with control and defect assertion

```java
@Test(groups = {"budget", "budget-readonly", "budget-defect"},
        description = "BC-062 (D-48): decimal days (1.5, 2.5) are rejected, whole days are accepted")
@Story("Activity form - days")
@Severity(SeverityLevel.CRITICAL)
@Description("Known defect D-48: 2.5 is accepted and Done is enabled. The document gives no message wording, so the check is that Done is "
        + "disabled and some validation feedback appears that was not there for the valid value. Controls: 2 and 3 whole days are accepted. Nothing is saved.")
public void bc062D48DecimalDaysRejected() {
    openFormWithLocation();
    makeFormValid();                                   // hard precondition: Done enabled, so a negative is not vacuous
    SoftAssert soft = new SoftAssert();

    drawer.enterDays("2");
    soft.assertTrue(drawer.isDoneEnabled(), "Control: 2 whole days should be accepted (Done enabled)");
    List<String> baseline = drawer.warnings();         // what the valid state already shows

    for (String decimal : List.of("1.5", "2.5")) {
        drawer.enterDays(decimal);
        List<String> feedback = newWarnings(baseline);
        soft.assertNotEquals(drawer.days(), decimal.replace(".", ""), "the field silently turned '" + decimal + "' into another number");
        soft.assertFalse(drawer.isDoneEnabled(), "D-48: decimal days '" + decimal + "' were accepted (field shows '" + drawer.days() + "')");
        soft.assertFalse(feedback.isEmpty(), "D-48: no validation feedback for '" + decimal + "' (the document gives no exact wording)");
    }
    drawer.enterDays("3");
    soft.assertTrue(drawer.isDoneEnabled(), "Control: whole days are accepted again after the decimals");
    soft.assertAll();
}
```

## Numeric state ledger (totals, counters)

```java
private final List<String> ledger = new ArrayList<>();
private void record(String state, Totals t) { ledger.add(state + " -> " + t); logger.info("[Totals ledger] {} -> {}", state, t); }
...
try {
    record("(1) A saved and checked", page.settledTotals(5));
    ...
} finally {
    Allure.addAttachment("Totals ledger", "text/plain", String.join(System.lineSeparator(), ledger));
}
soft.assertAll();
```
`Totals` is a record on the page object (header total, section total, grand total, selection text) with `isZero()` and `equalsAmount(long)`; an amount the page does not show counts as zero.

## Write test (one record, tracked, nothing deleted)

```java
drawer.clickDone();   // WRITE: saves the activity (a pre-draft)
BudgetRecords.trackStandalone("BC-090/BC-120", "pre-draft (Done pressed)", title, "D-55 totals test");
Assert.assertTrue(page.waitForCardSaved(1, 30), "The card should show 'Saved successfully' after Done: " + page.cardText(1));
BudgetRecords.trackStandalone("BC-090/BC-120", "pre-draft saved", title, "activityId=" + page.activityIdOfCard(1));
```
Track before the first assertion that can fail, so the record is accounted for even when the test fails.

## Blocked case

```java
@Test(groups = {"budget", "budget-blocked"}, description = "AL-022 'Property is already rented'")
public void al022PropertyAlreadyRented() {
    throw new SkipException("AL-022 needs a Rent/Property activity and a property code already in an active request; "
            + "the document marks it Not Run and no such test data is defined");
}
```
For catalogues (AL-040..053 error toasts that need backend faults) use a `@DataProvider` of id + message + reason so each id appears as its own skipped row.

## Page object method pairs

```java
public boolean isDeleteConfirmationShown() { return waitUntil(() -> visible(Create.text("Delete Activity?")), 5); }  // read-only
/** WRITES: saves the activity to the database (a pre-draft). */
public void clickDone() { click(find(Drawer.DONE)); }                                                            // WRITE, documented
```

## Elements helper

```java
public static String text(String contains) {
    return "//*[not(*)][not(self::script or self::style or self::noscript)][contains(normalize-space(), " + lit(contains) + ")]";
}
```

## Suite and Docker target

Suite XML: copy `testng-budget-ui.xml` (listeners + package) and describe the groups in the header comment. Docker: add the target next to `budget` in `docker/run-tests.sh` (credentials check -> exit 3; default group read-only; write group opt-in; not in `all`), then check the exit-code matrix with stubbed `mvn`/`java`.
