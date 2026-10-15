package ai.metayb.ui.budget;

import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The activity form of Create Budget (sections 8, 9 and 10 of the test document) with the Budget Alerts messages it
 * produces. The form is only ever filled, never saved: no test presses Done, so no record is created (the saving
 * workflow is in {@link BudgetDraftAndSubmitWorkflowTest}).
 *
 * Where the document records a known defect, the assertion states the documented EXPECTED behaviour, so the test fails
 * while the defect is present. The defect id is in the test name and in the assertion message.
 */
@Epic("Budget")
@Feature("Budget - Create: activity form")
public class BudgetActivityFormTest extends BudgetFormTestBase {

    private static final YearMonth MONTH = BudgetTestData.testMonth();
    private static final String MONTH_LABEL = BudgetTestData.monthLabel(MONTH);
    private static final int MONTH_DAYS = MONTH.lengthOfMonth();
    private static final BigDecimal THREE_THOUSAND = new BigDecimal("3000");
    private static final Pattern CLEAN_CITY = Pattern.compile("^[A-Za-z][A-Za-z .'-]{2,}$");

    /** Gives the form a title and valid days, so that Done would be enabled and a negative check is not vacuous. */
    private void makeFormValid() {
        drawer.enterTitle(BudgetTestData.uniqueTitle("form"));
        drawer.enterDays("3");
        Assert.assertTrue(drawer.isDoneEnabled(), "Precondition: a form with a title, a location and 3 days should be ready to save (Done enabled)");
    }

    // ---------------------------------------------------------------- location and lock

    @Test(groups = {"budget", "budget-readonly", "budget-alerts"}, description = "BC-052, BC-057, AL-009, AL-017: the form is locked and Done disabled until the last location level is chosen")
    @Story("Activity form - location")
    @Severity(SeverityLevel.CRITICAL)
    public void bc052Bc057Al009Al017FormLockedUntilLocation() {
        openFormBeforeLocation();
        SoftAssert soft = new SoftAssert();
        soft.assertFalse(drawer.isDoneEnabled(), "BC-052 Done should be disabled on an empty form");
        soft.assertTrue(drawer.isTextShown("Select location to unlock"), "AL-009 'Select location to unlock'");
        soft.assertTrue(drawer.isTextShown("Select the last location level above to unlock the form"), "AL-009 'Select the last location level above to unlock the form'");
        soft.assertTrue(drawer.isTextShown("Select a location above to load form fields."), "AL-009 'Select a location above to load form fields.'");
        soft.assertFalse(drawer.isLocationSelectEnabled("County"), "County should be locked until a State is chosen");
        soft.assertFalse(drawer.isLocationSelectEnabled("City"), "City should be locked until a County is chosen");

        drawer.selectLocation("State", BudgetTestData.STATE);
        soft.assertTrue(drawer.isFormLocked(), "BC-057 choosing only the State must keep the form locked");
        soft.assertTrue(drawer.isTextShown("Select the last location level above to unlock the form"), "BC-057 hint after choosing only the State");
        soft.assertFalse(drawer.isDoneEnabled(), "AL-017 Done stays disabled without a full location (the 'Please select a location first.' message cannot be reached)");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-055, BC-113: the Country is locked from Step 1 and the Activity Location chip shows the chosen city")
    @Story("Activity form - location")
    @Severity(SeverityLevel.NORMAL)
    public void bc055Bc113CountryLockedAndLocationChip() {
        openFormBeforeLocation();
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(drawer.isTextShown(BudgetTestData.COUNTRY), "BC-055 the Country '" + BudgetTestData.COUNTRY + "' should be shown");
        drawer.selectLocation(BudgetTestData.STATE, BudgetTestData.COUNTY, BudgetTestData.CITY);
        soft.assertTrue(drawer.isCountryLocked(), "BC-055 the Country carries a 'locked' badge");
        soft.assertTrue(drawer.isTextShown(BudgetTestData.CITY), "BC-113 the Activity Location chip shows '" + BudgetTestData.CITY + "'");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-056: State, County and City cascade, each level offering only its children")
    @Story("Activity form - location")
    @Severity(SeverityLevel.CRITICAL)
    public void bc056LocationCascade() {
        openFormBeforeLocation();
        SoftAssert soft = new SoftAssert();
        List<String> states = drawer.locationOptions("State");
        soft.assertTrue(states.contains(BudgetTestData.STATE), "States should offer " + BudgetTestData.STATE + ": " + states);
        drawer.selectLocation("State", BudgetTestData.STATE);
        List<String> counties = drawer.locationOptions("County");
        soft.assertTrue(counties.contains(BudgetTestData.COUNTY) && counties.contains(BudgetTestData.OTHER_COUNTY), "Counties of " + BudgetTestData.STATE + " should include both documented counties: " + counties);
        drawer.selectLocation("County", BudgetTestData.COUNTY);
        List<String> cities = drawer.locationOptions("City");
        soft.assertTrue(cities.contains(BudgetTestData.CITY), "Cities of " + BudgetTestData.COUNTY + " should include " + BudgetTestData.CITY + ": " + cities);
        drawer.selectLocation("City", BudgetTestData.CITY);
        soft.assertFalse(drawer.isFormLocked(), "Choosing the last level unlocks the form");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly", "budget-defect"}, description = "BC-058 (D-57): the Los Angeles County city list contains clean city names only")
    @Story("Activity form - location")
    @Severity(SeverityLevel.MINOR)
    @Description("Known defect D-57: the list contains 'CHENNAI', 'bn' and '@@@@@@@'. A clean name here is letters, spaces and . ' - only, at least 3 characters, "
            + "and not an Indian city under a US county; the document names those junk values.")
    public void bc058D57CleanCityNames() {
        openFormBeforeLocation();
        drawer.selectLocation("State", BudgetTestData.STATE);
        drawer.selectLocation("County", BudgetTestData.OTHER_COUNTY);
        List<String> cities = drawer.locationOptions("City");
        cities.removeIf(c -> c.isBlank() || c.startsWith("Select"));
        SoftAssert soft = new SoftAssert();
        for (String city : cities) {
            soft.assertTrue(CLEAN_CITY.matcher(city).matches(), "D-57 '" + city + "' is not a clean city name");
        }
        soft.assertFalse(cities.stream().anyMatch(c -> c.equalsIgnoreCase("CHENNAI")), "D-57 'CHENNAI' should not be a city of " + BudgetTestData.OTHER_COUNTY + ": " + cities);
        soft.assertAll();
    }

    // ---------------------------------------------------------------- title

    @Test(groups = {"budget", "budget-readonly", "budget-defect", "budget-alerts"}, description = "BC-053, AL-015 (D-68): a missing title disables Done and says 'Title is required'")
    @Story("Activity form - title")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Known defect D-68: Done is disabled but the Create drawer shows no 'Title is required' message (the Edit drawer does).")
    public void bc053Al015D68TitleRequired() {
        openFormWithLocation();
        drawer.enterDays("3");
        drawer.enterTitle("x");
        drawer.clearTitle();
        SoftAssert soft = new SoftAssert();
        soft.assertFalse(drawer.isDoneEnabled(), "BC-053 Done should be disabled without a title");
        soft.assertTrue(drawer.isTextShown("Title is required") || drawer.isTextShown("Please enter a title before saving."),
                "D-68 / AL-015 'Title is required' (or 'Please enter a title before saving.') should be shown");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-blocked"}, description = "BC-054: title length and special characters are limited and sanitised")
    @Story("Activity form - title")
    public void bc054TitleMaxLengthAndSpecialCharacters() {
        throw new SkipException("BC-054 expects the title to be 'limited & sanitised' but the document gives no maximum length or sanitising rule, so no assertion can be written.");
    }

    // ---------------------------------------------------------------- days

    @Test(groups = {"budget", "budget-readonly", "budget-alerts"}, description = "BC-059, BC-060, AL-011, AL-012, AL-013: days within and beyond the month")
    @Story("Activity form - days")
    @Severity(SeverityLevel.CRITICAL)
    public void bc059Bc060Al011Al012Al013DaysWithinAndBeyondMonth() {
        openFormWithLocation();
        makeFormValid();
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(drawer.isTextShown("Max " + MONTH_DAYS + " days in " + MONTH_LABEL), "AL-011 'Max " + MONTH_DAYS + " days in " + MONTH_LABEL + "'");

        drawer.clearDays();
        soft.assertTrue(drawer.isTextShown("Enter number of days before saving"), "AL-012 '⚠ Enter number of days before saving' for empty days");
        soft.assertFalse(drawer.isDoneEnabled(), "AL-012 Done disabled for empty days");

        drawer.enterDays("3");
        soft.assertTrue(drawer.isDoneEnabled(), "BC-059 3 days is accepted");
        soft.assertFalse(drawer.isTextShown("Enter number of days before saving"), "BC-059 the days warning is cleared for a valid value");

        drawer.enterDays(String.valueOf(MONTH_DAYS + 1));
        soft.assertTrue(drawer.isTextShown("Max " + MONTH_DAYS + " days allowed in " + MONTH_LABEL), "AL-013 'Max " + MONTH_DAYS + " days allowed in " + MONTH_LABEL + "'");
        soft.assertTrue(drawer.isTextShown(MONTH_LABEL + " has only " + MONTH_DAYS + " days available. Please enter " + MONTH_DAYS + " or fewer."),
                "AL-013 '" + MONTH_LABEL + " has only " + MONTH_DAYS + " days available. Please enter " + MONTH_DAYS + " or fewer.'");
        soft.assertFalse(drawer.isDoneEnabled(), "BC-060 days beyond the month disable Done");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly", "budget-defect", "budget-alerts"}, description = "BC-061, AL-012 (D-49): zero and negative days give the specific 'Days must be 1-N' error")
    @Story("Activity form - days")
    @Severity(SeverityLevel.NORMAL)
    @Description("Known defect D-49: both are blocked but with the generic 'Enter number of days before saving' instead of 'Days must be 1-N'.")
    public void bc061Al012D49ZeroAndNegativeDays() {
        openFormWithLocation();
        makeFormValid();
        SoftAssert soft = new SoftAssert();
        for (String value : List.of("0", "-2")) {
            drawer.enterDays(value);
            soft.assertFalse(drawer.isDoneEnabled(), "BC-061 days '" + value + "' should disable Done");
            soft.assertTrue(drawer.isTextShown("Days must be 1"), "D-49 days '" + value + "' should show the specific error 'Days must be 1–" + MONTH_DAYS + "'");
        }
        soft.assertAll();
    }

    /** Decimal values used for the whole-days checks: both are within the 3 budget days, so only their decimal part can make them invalid. */
    private static final List<String> DECIMAL_DAYS = List.of("1.5", "2.5");

    @Test(groups = {"budget", "budget-readonly", "budget-defect"}, description = "BC-062 (D-48): decimal days (1.5, 2.5) are rejected, whole days are accepted")
    @Story("Activity form - days")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Known defect D-48: 2.5 is accepted and Done is enabled. BC-062 expects 'Rejected (whole days only)'; the document gives no wording "
            + "for the message, so the check is that Done is disabled and that some validation feedback appears that was not there for the valid "
            + "value. Controls: 2 and 3 whole days are accepted. Nothing is saved.")
    public void bc062D48DecimalDaysRejected() {
        openFormWithLocation();
        makeFormValid();
        SoftAssert soft = new SoftAssert();

        drawer.enterDays("2");
        soft.assertTrue(drawer.isDoneEnabled(), "Control: 2 whole days should be accepted (Done enabled)");
        soft.assertEquals(drawer.days(), "2", "Control: the days field should keep the whole number 2");
        List<String> baseline = drawer.warnings();

        for (String decimal : DECIMAL_DAYS) {
            drawer.enterDays(decimal);
            boolean doneEnabled = drawer.isDoneEnabled();
            List<String> feedback = newWarnings(baseline);
            logger.info("[BC-062] days '{}' -> field shows '{}', Done enabled: {}, new warnings: {}", decimal, drawer.days(), doneEnabled, feedback);
            soft.assertNotEquals(drawer.days(), decimal.replace(".", ""), "D-48: the days field silently turned '" + decimal + "' into '" + drawer.days() + "'");
            soft.assertFalse(doneEnabled, "D-48: decimal days '" + decimal + "' were accepted (Done is enabled, field shows '" + drawer.days()
                    + "'); whole days only is expected");
            soft.assertFalse(feedback.isEmpty(), "D-48: no validation feedback is shown for decimal days '" + decimal + "' (the document gives no exact wording)");
        }

        drawer.enterDays("3");
        soft.assertTrue(drawer.isDoneEnabled(), "Control: after the decimal values, 3 whole days should be accepted again (Done enabled)");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly", "budget-defect"}, description = "BC-062 (D-48), manpower days: decimal days (1.5, 2.5) are rejected, whole days are accepted")
    @Story("Activity form - manpower")
    @Severity(SeverityLevel.CRITICAL)
    @Description("D-48 for the manpower days column. The document records D-48 for the Days field; applying 'whole days only' to the manpower days "
            + "(1 x 500 x days against 3 budget days) follows the same rule but is not separately documented, so a failure here is reported as "
            + "an unconfirmed requirement. Controls: 2 whole days give a line total of 1,000 and Done stays enabled. Nothing is saved.")
    public void bc062D48DecimalManpowerDaysRejected() {
        openFormWithLocation();
        makeFormValid();
        drawer.selectDesignation(1, BudgetTestData.DESIGNATION);
        drawer.enterUnit(1, "1");
        SoftAssert soft = new SoftAssert();

        drawer.enterManpowerDays(1, "2");
        soft.assertTrue(drawer.isDoneEnabled(), "Control: 2 whole manpower days should be accepted (Done enabled)");
        soft.assertEquals(drawer.manpowerDays(1), "2", "Control: the manpower days field should keep the whole number 2");
        soft.assertEquals(amountOf(drawer.lineTotal(1)).compareTo(new BigDecimal("1000")), 0,
                "Control: the line total should be 1 x 500 x 2 = 1,000 but is '" + drawer.lineTotal(1) + "'");
        List<String> baseline = drawer.warnings();

        for (String decimal : DECIMAL_DAYS) {
            drawer.enterManpowerDays(1, decimal);
            boolean doneEnabled = drawer.isDoneEnabled();
            List<String> feedback = newWarnings(baseline);
            logger.info("[BC-062] manpower days '{}' -> field shows '{}', line total '{}', Done enabled: {}, new warnings: {}",
                    decimal, drawer.manpowerDays(1), drawer.lineTotal(1), doneEnabled, feedback);
            soft.assertNotEquals(drawer.manpowerDays(1), decimal.replace(".", ""),
                    "D-48: the manpower days field silently turned '" + decimal + "' into '" + drawer.manpowerDays(1) + "' (a different number)");
            soft.assertFalse(doneEnabled, "D-48: decimal manpower days '" + decimal + "' were accepted (Done is enabled, field shows '"
                    + drawer.manpowerDays(1) + "', line total '" + drawer.lineTotal(1) + "'); whole days only is expected");
            soft.assertFalse(feedback.isEmpty(), "D-48: no validation feedback is shown for decimal manpower days '" + decimal + "' (the document gives no exact wording)");
        }

        drawer.enterManpowerDays(1, "3");
        soft.assertTrue(drawer.isDoneEnabled(), "Control: after the decimal values, 3 whole manpower days should be accepted again (Done enabled)");
        soft.assertAll();
    }

    /** The warnings on the form now that were not there in the baseline (the valid state). */
    private List<String> newWarnings(List<String> baseline) {
        List<String> now = new java.util.ArrayList<>(drawer.warnings());
        now.removeAll(baseline);
        return now;
    }

    private static BigDecimal amountOf(String text) {
        BigDecimal amount = ai.metayb.ui.pages.budget.BudgetPageBase.parseAmount(text);
        return amount == null ? new BigDecimal("-1") : amount;
    }

    @Test(groups = {"budget", "budget-readonly", "budget-defect"}, description = "BC-063 (D-53): Reset asks for confirmation and clears the form")
    @Story("Activity form - buttons")
    @Severity(SeverityLevel.MINOR)
    @Description("Known defect D-53: no confirmation, only days and sections are cleared (title and location are kept). The document leaves open "
            + "whether title and location must be cleared, so only the confirmation and the cleared days are asserted.")
    public void bc063D53ResetConfirmsAndClears() {
        openFormWithLocation();
        makeFormValid();
        drawer.reset();
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(page.isConfirmationShown(3), "D-53 Reset should ask for confirmation");
        soft.assertEquals(drawer.days(), "", "BC-063 Reset should clear the days");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly", "budget-defect", "budget-alerts"}, description = "BC-064, AL-032 (D-52): closing the form with unsaved data asks 'Discard changes?'")
    @Story("Activity form - buttons")
    @Severity(SeverityLevel.NORMAL)
    @Description("Known defect D-52: the form closes silently and the card shows 'Not saved' with the title kept.")
    public void bc064Al032D52CloseWithUnsavedDataWarns() {
        openFormWithLocation();
        drawer.enterTitle(BudgetTestData.uniqueTitle("close"));
        drawer.close();
        Assert.assertTrue(page.isConfirmationShown(3), "D-52 / AL-032 closing the form with unsaved data should ask 'Discard changes?' but it closed silently");
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-065, BC-075, BC-074: the manpower line is Unit x Rate x Days and the requested amount updates live")
    @Story("Activity form - manpower")
    @Severity(SeverityLevel.CRITICAL)
    public void bc065Bc074Bc075ManpowerLineAndLiveTotals() {
        openFormWithLocation();
        drawer.enterDays("3");
        drawer.selectDesignation(1, BudgetTestData.DESIGNATION);
        SoftAssert soft = new SoftAssert();
        soft.assertEquals(drawer.rate(1), String.valueOf(BudgetTestData.DESIGNATION_RATE), "BC-074 the rate fills from the standard rate");
        soft.assertTrue(drawer.standardRate(1).contains(String.valueOf(BudgetTestData.DESIGNATION_RATE)), "BC-074 STD. RATE shows the standard rate: " + drawer.standardRate(1));
        soft.assertTrue(drawer.isStandardRateReadOnly(1), "BC-074 STD. RATE is read-only");

        drawer.enterUnit(1, "2");
        drawer.enterManpowerDays(1, "3");
        soft.assertEquals(ai.metayb.ui.pages.budget.BudgetPageBase.parseAmount(drawer.lineTotal(1)).compareTo(THREE_THOUSAND), 0, "BC-075 line total 2 x 500 x 3 = 3,000 but is " + drawer.lineTotal(1));
        soft.assertEquals(drawer.manpowerTotal().compareTo(THREE_THOUSAND), 0, "BC-075 Manpower Total");
        soft.assertEquals(drawer.headerRequested().compareTo(THREE_THOUSAND), 0, "BC-065 header 'Requested'");
        soft.assertEquals(drawer.footerRequested().compareTo(THREE_THOUSAND), 0, "BC-065 footer 'Requested Amount'");
        soft.assertAll();

        drawer.close();
        drawer.waitUntilClosed();
        SoftAssert pageTotals = new SoftAssert();
        pageTotals.assertEquals(page.headerTotalRequested().compareTo(THREE_THOUSAND), 0, "BC-075 page 'Total Requested'");
        pageTotals.assertEquals(page.grandTotal().compareTo(THREE_THOUSAND), 0, "BC-075 'Grand Total'");
        pageTotals.assertAll();
    }

    // ---------------------------------------------------------------- product

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-066: the Product list shows each product with its standard rate")
    @Story("Activity form - product")
    @Severity(SeverityLevel.NORMAL)
    public void bc066ProductListShowsStandardRates() {
        openFormWithLocation();
        List<String> options = drawer.productOptions();
        Assert.assertFalse(options.isEmpty(), "The Product list should offer products");
        String documented = BudgetTestData.PRODUCT + " " + BudgetTestData.CURRENCY + BudgetTestData.PRODUCT_RATE;
        Assert.assertTrue(options.stream().anyMatch(o -> o.equals(documented)), "The list should show '" + documented + "': " + options);
        drawer.selectProduct(1, BudgetTestData.PRODUCT);
        Assert.assertTrue(drawer.selectedProduct(1).startsWith(BudgetTestData.PRODUCT), "The product should be selected but shows '" + drawer.selectedProduct(1) + "'");
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-067: planned quantity accepts numbers only")
    @Story("Activity form - product")
    @Severity(SeverityLevel.NORMAL)
    public void bc067PlannedQuantityNumericOnly() {
        openFormWithLocation();
        drawer.selectProduct(1, BudgetTestData.PRODUCT);
        SoftAssert soft = new SoftAssert();
        drawer.enterPlannedQuantityForSampling(1, "-5");
        soft.assertTrue(drawer.plannedQuantityForSampling(1).matches("\\d*"), "BC-067 '-5' should be rejected but the field shows '" + drawer.plannedQuantityForSampling(1) + "'");
        drawer.enterPlannedQuantityForSampling(1, "abc");
        soft.assertEquals(drawer.plannedQuantityForSampling(1), "", "BC-067 'abc' should be rejected");
        drawer.enterPlannedQuantityForSampling(1, "100");
        soft.assertEquals(drawer.plannedQuantityForSampling(1), "100", "BC-067 '100' should be accepted");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-069, BC-070, BC-071: product rows can be added and deleted; a single row has no delete")
    @Story("Activity form - product")
    @Severity(SeverityLevel.NORMAL)
    public void bc069Bc070Bc071ProductRows() {
        openFormWithLocation();
        SoftAssert soft = new SoftAssert();
        soft.assertEquals(drawer.productRowCount(), 1, "One product row to start");
        soft.assertEquals(drawer.productDeleteIconCount(), 0, "BC-071 a single product row has no delete icon");
        drawer.addProductRow();
        soft.assertEquals(drawer.productRowCount(), 2, "BC-069 Add Product adds a row");
        soft.assertTrue(drawer.productDeleteIconCount() >= 1, "BC-069 the added row has a delete icon");
        drawer.deleteProductRow(drawer.productDeleteIconCount());
        soft.assertEquals(drawer.productRowCount(), 1, "BC-070 the row is removed");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-072: the same product in two rows is blocked or warned")
    @Story("Activity form - product")
    @Severity(SeverityLevel.NORMAL)
    @Description("The document records this as not run and does not define the warning, so any of: the second selection is refused, a warning is shown, or Done is disabled, satisfies it.")
    public void bc072DuplicateProductInTwoRows() {
        openFormWithLocation();
        makeFormValid();
        drawer.selectProduct(1, BudgetTestData.PRODUCT);
        drawer.addProductRow();
        drawer.selectProduct(2, BudgetTestData.PRODUCT);
        boolean refused = !drawer.selectedProduct(2).startsWith(BudgetTestData.PRODUCT);
        boolean warned = !drawer.warnings().isEmpty();
        boolean doneBlocked = !drawer.isDoneEnabled();
        Assert.assertTrue(refused || warned || doneBlocked, "BC-072 the same product twice was neither refused, warned about, nor blocked");
    }

    @Test(groups = {"budget", "budget-blocked", "budget-defect"}, description = "BC-068 (D-56): product quantity x rate contributes to the requested amount")
    @Story("Activity form - product")
    public void bc068D56ProductAmountInRequestedAmount() {
        throw new SkipException("BC-068 / D-56: the expected formula needs confirmation from the BA ('Confirm with BA whether product cost adds to requested amount'). "
                + "The document defines no expected amount, so none is invented; the observed behaviour is that the requested amount stays manpower-only.");
    }

    // ---------------------------------------------------------------- manpower

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-073: the Designation list shows each configured designation with its standard rate")
    @Story("Activity form - manpower")
    @Severity(SeverityLevel.NORMAL)
    public void bc073DesignationListWithStandardRates() {
        openFormWithLocation();
        List<String> options = drawer.designationOptions();
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(options.contains(BudgetTestData.DESIGNATION + " " + BudgetTestData.CURRENCY + BudgetTestData.DESIGNATION_RATE),
                "Designation list should show '" + BudgetTestData.DESIGNATION + " " + BudgetTestData.CURRENCY + BudgetTestData.DESIGNATION_RATE + "': " + options);
        soft.assertTrue(options.contains(BudgetTestData.OTHER_DESIGNATION + " " + BudgetTestData.CURRENCY + "1,000"),
                "Designation list should show '" + BudgetTestData.OTHER_DESIGNATION + " " + BudgetTestData.CURRENCY + "1,000': " + options);
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-076, BC-079: unit and rate reject text and negative values")
    @Story("Activity form - manpower")
    @Severity(SeverityLevel.MINOR)
    public void bc076Bc079UnitAndRateRejectText() {
        openFormWithLocation();
        drawer.selectDesignation(1, BudgetTestData.DESIGNATION);
        SoftAssert soft = new SoftAssert();
        drawer.enterUnit(1, "abc");
        soft.assertEquals(drawer.unit(1), "", "BC-076 text is rejected in Unit");
        drawer.enterUnit(1, "-1");
        soft.assertTrue(drawer.unit(1).matches("\\d*"), "BC-076 a negative Unit is rejected but the field shows '" + drawer.unit(1) + "'");
        drawer.enterRate(1, "abc");
        soft.assertEquals(drawer.rate(1), "", "BC-079 text is rejected in Rate");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly", "budget-defect", "budget-alerts"}, description = "BC-077, AL-014 (D-50): manpower days above the budget days show a field-level error")
    @Story("Activity form - manpower")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Known defect D-50: Done is disabled but only the footer '⚠ Fix 1 error(s) before saving' appears, with no field message 'Cannot exceed 3 budget days'.")
    public void bc077Al014D50ManpowerDaysExceedBudgetDays() {
        openFormWithLocation();
        makeFormValid();
        drawer.selectDesignation(1, BudgetTestData.DESIGNATION);
        drawer.enterUnit(1, "2");
        drawer.enterManpowerDays(1, "5");
        SoftAssert soft = new SoftAssert();
        soft.assertFalse(drawer.isDoneEnabled(), "BC-077 Done should be disabled when manpower days exceed the budget days");
        soft.assertTrue(drawer.isTextShown("Fix 1 error(s) before saving"), "AL-014 footer '⚠ Fix 1 error(s) before saving'");
        soft.assertTrue(drawer.isTextShown("Cannot exceed 3 budget days"), "D-50 the field-level error 'Cannot exceed 3 budget days' should be shown");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-078: a rate above or below the standard rate is allowed when the restriction is off")
    @Story("Activity form - manpower")
    @Severity(SeverityLevel.NORMAL)
    public void bc078RateAboveAndBelowStandard() {
        openFormWithLocation();
        makeFormValid();
        drawer.selectDesignation(1, BudgetTestData.DESIGNATION);
        drawer.enterUnit(1, "2");
        drawer.enterManpowerDays(1, "3");
        SoftAssert soft = new SoftAssert();
        drawer.enterRate(1, "600");
        soft.assertEquals(ai.metayb.ui.pages.budget.BudgetPageBase.parseAmount(drawer.lineTotal(1)).compareTo(new BigDecimal("3600")), 0, "BC-078 rate 600 gives 2 x 600 x 3 = 3,600");
        soft.assertTrue(drawer.isDoneEnabled(), "BC-078 a rate above the standard rate is allowed (Restrict to Standard Rate is off for " + BudgetTestData.ACTIVITY + ")");
        drawer.enterRate(1, "100");
        soft.assertEquals(ai.metayb.ui.pages.budget.BudgetPageBase.parseAmount(drawer.lineTotal(1)).compareTo(new BigDecimal("600")), 0, "BC-078 rate 100 gives 2 x 100 x 3 = 600");
        soft.assertTrue(drawer.isDoneEnabled(), "BC-078 a rate below the standard rate is allowed");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly", "budget-defect"}, description = "BC-080 (D-46): a 14-digit rate is refused by a maximum limit")
    @Story("Activity form - manpower")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Known defect D-46: the rate 99999999999999 is accepted (line total 599,999,999,999,994). The document says only 'Max limit enforced' and gives no limit value, "
            + "so refusing the value in any way (Done disabled, an error, or the field limiting its length) satisfies it.")
    public void bc080D46VeryLargeRateRefused() {
        openFormWithLocation();
        makeFormValid();
        drawer.selectDesignation(1, BudgetTestData.DESIGNATION);
        drawer.enterUnit(1, "2");
        drawer.enterManpowerDays(1, "3");
        drawer.enterRate(1, "99999999999999");
        boolean valueKept = "99999999999999".equals(drawer.rate(1));
        boolean doneBlocked = !drawer.isDoneEnabled();
        Assert.assertTrue(!valueKept || doneBlocked, "D-46: the 14-digit rate was accepted (Done enabled, line total " + drawer.lineTotal(1) + "); a maximum limit is expected");
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-081, BC-083: manpower rows can be added and deleted and the totals are unchanged by removing an empty row")
    @Story("Activity form - manpower")
    @Severity(SeverityLevel.NORMAL)
    public void bc081Bc083ManpowerRows() {
        openFormWithLocation();
        drawer.enterDays("3");
        drawer.selectDesignation(1, BudgetTestData.DESIGNATION);
        drawer.enterUnit(1, "2");
        drawer.enterManpowerDays(1, "3");
        BigDecimal before = drawer.manpowerTotal();
        SoftAssert soft = new SoftAssert();
        drawer.addManpowerRow();
        soft.assertEquals(drawer.manpowerRowCount(), 2, "BC-081 Add Manpower adds a row");
        soft.assertEquals(drawer.standardRate(2), "—", "BC-081 the new row's STD. RATE is '—'");
        soft.assertTrue(drawer.manpowerDeleteIconCount() >= 1, "BC-081 the added row has a delete icon");
        drawer.deleteManpowerRow(drawer.manpowerDeleteIconCount());
        soft.assertEquals(drawer.manpowerRowCount(), 1, "BC-083 the row is removed");
        soft.assertEquals(drawer.manpowerTotal().compareTo(before), 0, "BC-083 the totals are unchanged");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-blocked"}, description = "BC-082 (D-51): empty extra rows are ignored or flagged on save")
    @Story("Activity form - rows")
    public void bc082D51EmptyExtraRowsOnSave() {
        throw new SkipException("BC-082 needs Done, which saves a record; D-51 is not one of the defects in scope, so no extra record is created for it.");
    }

    @Test(groups = {"budget", "budget-blocked"}, description = "BC-084: every configured designation is offered in the dropdown")
    @Story("Activity form - manpower")
    public void bc084DesignationMissingFromDropdown() {
        throw new SkipException("BC-084 compares the dropdown with the activity master (API masterData, 'Supervisor' id 318). It needs an API call the UI suite does not make, "
                + "and the document asks to 'confirm if inactive'.");
    }
}
