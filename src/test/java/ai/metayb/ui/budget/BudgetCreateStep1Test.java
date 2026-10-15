package ai.metayb.ui.budget;

import ai.metayb.ui.pages.budget.BudgetCreatePage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Create Budget Step 1 (Country and Budget Month), the month entry and the Add Activity picker: sections 5, 6 and the
 * picker part of section 7 of the test document, with the Budget Alerts hints they produce. Nothing is saved: no test
 * presses Done, Save as Draft or Submit.
 *
 * The native month-picker popup (calendar icon, Clear, This month) cannot be driven reliably by automation, as the
 * document itself notes, so those cases are listed as skipped; the month value is set directly on the input (value plus input/change events), which is what these tests use.
 */
@Epic("Budget")
@Feature("Budget - Create: Step 1, month and activity picker")
public class BudgetCreateStep1Test extends BudgetFormTestBase {

    @Test(groups = {"budget", "budget-readonly", "budget-defect"}, description = "BC-037 (D-57): the Country list offers the business unit's countries without duplicates")
    @Story("Country")
    @Severity(SeverityLevel.NORMAL)
    @Description("Known defect D-57: 'USA' and 'United States' are both listed, and 'Africa' is offered as a country.")
    public void bc037D57CountryDropdownListsCountries() {
        page = openCreateBudget();
        List<String> countries = page.countryOptions();
        Assert.assertFalse(countries.isEmpty(), "The Country list should offer countries");
        Set<String> unique = new HashSet<>(countries);
        SoftAssert soft = new SoftAssert();
        soft.assertEquals(unique.size(), countries.size(), "No country should be listed twice: " + countries);
        soft.assertFalse(countries.contains("USA") && countries.contains(BudgetTestData.COUNTRY),
                "D-57: both 'USA' and 'United States' are listed: " + countries);
        soft.assertFalse(countries.contains("Africa"), "D-57: 'Africa' is a continent, not a country, but it is offered: " + countries);
        soft.assertTrue(countries.contains(BudgetTestData.COUNTRY), "'" + BudgetTestData.COUNTRY + "' should be offered: " + countries);
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly", "budget-alerts"}, description = "BC-038, BC-114, AL-002: selecting a Country unlocks Budget Month and shows the month hints")
    @Story("Country and month")
    @Severity(SeverityLevel.CRITICAL)
    public void bc038Bc114Al002CountryUnlocksBudgetMonth() {
        page = openCreateBudget();
        page.selectCountry(BudgetTestData.COUNTRY);
        SoftAssert soft = new SoftAssert();
        soft.assertEquals(page.selectedCountry(), BudgetTestData.COUNTRY, "Selected country");
        soft.assertTrue(page.isMonthEnabled(), "Budget Month should be enabled once a Country is selected");
        soft.assertEquals(page.monthInputType(), "month", "Budget Month should be a month input");
        soft.assertEquals(page.monthMin(), YearMonth.now().toString(), "The earliest selectable month is the current month");
        soft.assertTrue(page.isTextShown("② Select Budget Month to add activities"), "AL-002 hint '② Select Budget Month to add activities'");
        soft.assertTrue(page.isTextShown("Select a Budget Month first to add activities"), "AL-002 'Select a Budget Month first to add activities'");
        soft.assertFalse(page.isAddActivityEnabled(), "AL-007: Add Activity stays disabled until a month is chosen (the toast cannot be triggered)");
        soft.assertTrue(page.isTextShown(BudgetTestData.COUNTRY), "BC-114 the header should show a '" + BudgetTestData.COUNTRY + "' chip");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-039, BC-133: a past month is not accepted and Add Activity stays disabled")
    @Story("Budget month")
    @Severity(SeverityLevel.CRITICAL)
    public void bc039Bc133PastMonthRejected() {
        page = openCreateBudget();
        page.selectCountry(BudgetTestData.COUNTRY);
        YearMonth past = BudgetTestData.pastMonth();
        page.typeBudgetMonth(past);
        boolean cleared = page.waitForMonthNotEqual(past, 3);
        Assert.assertTrue(cleared, "A past month (" + past + ") should not be accepted, but the field kept it");
        Assert.assertFalse(page.isAddActivityEnabled(), "Add Activity should stay disabled after a past month");
    }

    @Test(groups = {"budget", "budget-readonly", "budget-alerts"}, description = "BC-040, BC-115, BC-133, AL-003: a future month is accepted and the days-available hint follows it")
    @Story("Budget month")
    @Severity(SeverityLevel.CRITICAL)
    public void bc040Bc115Al003FutureMonthAccepted() {
        page = openCreateBudget();
        page.selectCountry(BudgetTestData.COUNTRY);
        SoftAssert soft = new SoftAssert();
        for (YearMonth month : List.of(BudgetTestData.testMonth(), BudgetTestData.monthAfter(BudgetTestData.testMonth(), 1), BudgetTestData.nextFebruary())) {
            page.setBudgetMonth(month);
            String label = BudgetTestData.monthLabel(month);
            soft.assertTrue(page.isTextShown("Budget Month: " + label), "Header chip 'Budget Month: " + label + "'");
            soft.assertTrue(page.isTextShown(month.lengthOfMonth() + " days available in " + label),
                    "AL-003 hint '" + month.lengthOfMonth() + " days available in " + label + "'");
            soft.assertTrue(page.isAddActivityEnabled(), "Add Activity should be enabled for " + month);
        }
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly", "budget-alerts"}, description = "AL-004: with a month set and nothing added, the empty-activities state is shown")
    @Story("Alerts")
    @Severity(SeverityLevel.MINOR)
    public void al004EmptyActivitiesState() {
        openCreateWithMonth();
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(page.isTextShown("No activities added yet"), "AL-004 'No activities added yet'");
        soft.assertTrue(page.isTextShown("Click \"Add Activity\" to add standard activities (up to 25)"),
                "AL-004 'Click \"Add Activity\" to add standard activities (up to 25)'");
        soft.assertEquals(page.counterText(), "0/25", "Counter");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly", "budget-defect"}, description = "BC-042, BC-115 (D-61): changing the month after an activity was added updates every label and warns")
    @Story("Budget month")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Known defect D-61: all labels update (chip, hint, card BM, drawer max days) but no warning is shown on the change. "
            + "The document does not define the warning's wording, so any warning surface (dialog, alert or toast) is accepted.")
    public void bc042Bc115D61ChangeMonthAfterActivityAdded() {
        openFormBeforeLocation();
        drawer.close();
        drawer.waitUntilClosed();
        YearMonth newMonth = BudgetTestData.monthAfter(BudgetTestData.testMonth(), 1);
        page.typeBudgetMonth(newMonth);
        String label = BudgetTestData.monthLabel(newMonth);
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(page.waitForMonthEquals(newMonth, 5), "The month field should accept " + newMonth);
        soft.assertTrue(page.isTextShown("Budget Month: " + label), "Header chip follows the new month");
        soft.assertTrue(page.isTextShown(newMonth.lengthOfMonth() + " days available in " + label), "Days-available hint follows the new month");
        soft.assertTrue(page.cardText(1).contains("BM: " + label), "The card's budget month follows: " + page.cardText(1));
        soft.assertTrue(page.isMonthChangeWarningShown(), "D-61: no warning is shown when the month is changed after an activity was added");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly", "budget-alerts"}, description = "BC-045, BC-046, BC-047, AL-008: the Add Activity picker lists activities, searches them and reports no match")
    @Story("Activity picker")
    @Severity(SeverityLevel.CRITICAL)
    public void bc045Bc046Bc047Al008ActivityPicker() {
        openCreateWithMonth();
        page.openActivityPicker();
        List<String> all = page.pickerActivityNames();
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(all.contains(BudgetTestData.ACTIVITY), "BC-045 the picker should list '" + BudgetTestData.ACTIVITY + "'");

        page.searchPicker("event sampling");
        List<String> found = page.pickerActivityNames();
        soft.assertTrue(found.contains(BudgetTestData.ACTIVITY), "BC-046 searching 'event sampling' should find '" + BudgetTestData.ACTIVITY + "': " + found);
        for (String name : found) {
            soft.assertTrue(name.toLowerCase().contains("event sampling"), "BC-046 only matching activities should be listed, but '" + name + "' is");
        }

        page.searchPicker("zzqq-nomatch");
        soft.assertTrue(page.isNoActivitiesFoundShown(), "BC-047 / AL-008 'No activities found'");
        soft.assertAll();
        page.closePicker();
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-049, BC-050, BC-116: adding an activity adds a card and opens its form; adding it twice gives two independent cards")
    @Story("Add activity")
    @Severity(SeverityLevel.CRITICAL)
    public void bc049Bc050Bc116AddActivityAddsCards() {
        openCreateWithMonth();
        String monthLabel = BudgetTestData.monthLabel(BudgetTestData.testMonth());
        drawer = page.addActivity(BudgetTestData.ACTIVITY);
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(drawer.isOpen(), "BC-049 the activity form should open");
        soft.assertEquals(drawer.heading(), BudgetTestData.ACTIVITY, "BC-049 form title");
        drawer.close();
        drawer.waitUntilClosed();
        soft.assertEquals(page.cardCount(), 1, "BC-049 one card");
        soft.assertEquals(page.counterText(), "1/25", "BC-116 counter");
        soft.assertTrue(page.isTextShown("(1 added)"), "BC-116 '(1 added)'");
        String card = page.cardText(1);
        soft.assertTrue(card.contains("Title missing") && card.contains("Dates not set") && card.contains("BM: " + monthLabel),
                "BC-049 / AL-016 the card should show 'Title missing', 'Dates not set' and 'BM: " + monthLabel + "': " + card);
        soft.assertTrue(card.contains("Not saved") && card.contains("Click Edit to open form"),
                "AL-026 the unsaved card shows 'Not saved' and 'Click Edit to open form': " + card);
        soft.assertTrue(page.readyText().contains("0 of 1") && page.readyText().contains("ready"),
                "AL-027 '0 of 1 checked activity ready' but shows '" + page.readyText() + "'");

        drawer = page.addActivity(BudgetTestData.ACTIVITY);
        drawer.close();
        drawer.waitUntilClosed();
        soft.assertEquals(page.cardCount(), 2, "BC-050 the same activity can be added twice");
        soft.assertEquals(page.counterText(), "2/25", "BC-050 counter");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly", "budget-slow", "budget-alerts"},
            description = "BC-051, AL-005, AL-006: at most 25 activities; at 25/25 'Limit reached' is shown and Add Activity disappears")
    @Story("Activity limit")
    @Severity(SeverityLevel.NORMAL)
    @Description("Adds 25 unsaved cards (nothing is saved), so it is slow and kept out of the smoke group (D-71). The 'Maximum of 25 activities "
            + "allowed per bulk budget.' toast cannot be triggered because the button is no longer offered (AL-006).")
    public void bc051Al005Al006MaximumTwentyFiveActivities() {
        openCreateWithMonth();
        for (int i = 1; i <= BudgetCreatePage.MAX_ACTIVITIES; i++) {
            drawer = page.addActivity(BudgetTestData.ACTIVITY);
            drawer.close();
            drawer.waitUntilClosed();
        }
        SoftAssert soft = new SoftAssert();
        soft.assertEquals(page.cardCount(), BudgetCreatePage.MAX_ACTIVITIES, "25 cards");
        soft.assertEquals(page.counterText(), "25/25", "Counter");
        soft.assertTrue(page.isLimitReachedShown(), "AL-005 'Limit reached (25/25)'");
        soft.assertEquals(page.addActivityButtonCount(), 0, "AL-005 / AL-006 both Add Activity buttons are removed at the limit");
        soft.assertAll();
    }

    // ---------------------------------------------------------------- listed, not automated

    @Test(groups = {"budget", "budget-blocked"}, description = "BC-041: far-future month is rejected or limited")
    @Story("Budget month")
    public void bc041FarFutureMonthLimit() {
        throw new SkipException("BC-041: the expected result is 'Rejected or limited per business rule (confirm max horizon)'. The document does not give the maximum horizon, so no assertion can be written.");
    }

    @Test(groups = {"budget", "budget-blocked"}, description = "BC-048: an inactive or non-budget activity is not listed in the picker")
    @Story("Activity picker")
    public void bc048InactiveActivityNotListed() {
        throw new SkipException("BC-048 needs an activity that is inactive or has Budget Required = No; the document names none.");
    }

    @Test(groups = {"budget", "budget-blocked"}, description = "BC-126, BC-127, BC-128, BC-129, BC-130, BC-131, BC-132: native month picker popup")
    @Story("Month picker")
    public void bc126To132NativeMonthPickerPopup() {
        throw new SkipException("BC-126..BC-132 drive the browser's native month-picker popup (calendar icon, Clear, This month, year navigation). The document records that "
                + "automation cannot register those clicks (BC-129/130: 'verify manually'; BC-131: simulated clear reverts, D-67). Typed entry is covered by BC-039/040/133.");
    }
}
