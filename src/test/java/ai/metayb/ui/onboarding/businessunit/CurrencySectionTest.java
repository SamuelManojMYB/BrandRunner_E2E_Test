package ai.metayb.ui.onboarding.businessunit;

import ai.metayb.ui.pages.onboarding.CreateBusinessUnitCurrencySection;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitDialog;
import ai.metayb.ui.pages.onboarding.DashboardPage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.OnboardingAuthenticatedTest;

import java.util.List;

/**
 * Currency section of the Create Business Unit dialog, covering only behavior verified on the live portal.
 * Every test opens the dialog itself from a fresh dashboard and finishes by leaving it. Nothing is created:
 * Create is never clicked, and selecting a currency only changes unsaved form state.
 *
 * The test values are INR and USD. The option list is deliberately not asserted in full (it is data the portal
 * may extend); tests only require the currencies they use to be offered.
 */
@Epic("Onboarding Portal")
@Feature("Create Business Unit - Currency")
public class CurrencySectionTest extends OnboardingAuthenticatedTest {

    private static final String INR = "INR";
    private static final String USD = "USD";
    private static final String REQUIRED = "Currency is required.";

    private DashboardPage dashboard;
    private CreateBusinessUnitDialog dialog;
    private CreateBusinessUnitCurrencySection currency;

    /** Opens the dialog from a fresh dashboard and shows the Currency section. */
    private void openCurrencySection() {
        dashboard = openDashboard();
        dashboard.clickAddBusinessUnit();
        dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        dialog.selectSection(CreateBusinessUnitCurrencySection.SECTION);
        currency = new CreateBusinessUnitCurrencySection();
    }

    @Test(groups = {"regression", "currency"}, description = "CUR-001 Currency section opens correctly")
    @Story("Section")
    @Severity(SeverityLevel.CRITICAL)
    public void cur001_currencySectionOpensCorrectly() {
        openCurrencySection();

        Assert.assertEquals(dialog.getSelectedSection(), "Currency", "Selected navigation row");
        Assert.assertEquals(currency.getCurrencyHeading(), "Currency");
        Assert.assertEquals(currency.getCurrencyDescription(), "Set the default currency for this business unit.");
        Assert.assertEquals(currency.getCurrencyLabel(), "Currency *");
        Assert.assertTrue(currency.isCurrencyFieldDisplayed(), "Currency field should be displayed");
        Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard");
    }

    @Test(groups = {"regression", "currency"}, description = "CUR-002 Currency starts empty with no error")
    @Story("Initial state")
    @Severity(SeverityLevel.NORMAL)
    public void cur002_currencyStartsEmptyWithNoError() {
        openCurrencySection();

        Assert.assertEquals(currency.getSelectedCurrency(), "", "Nothing is preselected");
        Assert.assertFalse(currency.isCurrencyErrorDisplayed(), "No validation error initially");
        Assert.assertFalse(currency.isCurrencyListOpen(), "The list starts closed");
    }

    @Test(groups = {"regression", "currency"}, description = "CUR-003 Focus and blur shows no error")
    @Story("Validation")
    @Severity(SeverityLevel.NORMAL)
    public void cur003_focusAndBlurWithoutSelectingShowsNoError() {
        openCurrencySection();

        currency.openCurrencyList();
        currency.blurCurrency();

        Assert.assertFalse(currency.isCurrencyListOpen(), "Leaving the field closes the list");
        Assert.assertFalse(currency.isCurrencyErrorDisplayed(), "Focusing and leaving without selecting must not show an error");
        Assert.assertEquals(currency.getSelectedCurrency(), "");
    }

    @Test(groups = {"regression", "currency"}, description = "CUR-004 Dropdown offers INR and USD")
    @Story("Dropdown")
    @Severity(SeverityLevel.NORMAL)
    public void cur004_dropdownOffersKnownCurrencies() {
        openCurrencySection();

        currency.openCurrencyList();

        Assert.assertTrue(currency.isCurrencyListOpen(), "The list should be open");
        List<String> options = currency.getCurrencyOptions();
        Assert.assertTrue(options.stream().anyMatch(o -> o.startsWith(INR + " (")), "INR should be offered, options were: " + options);
        Assert.assertTrue(options.stream().anyMatch(o -> o.startsWith(USD + " (")), "USD should be offered, options were: " + options);
    }

    @Test(groups = {"regression", "currency"}, description = "CUR-005 Select INR by clicking")
    @Story("Selection")
    @Severity(SeverityLevel.CRITICAL)
    public void cur005_selectInrByClicking() {
        openCurrencySection();

        currency.selectCurrency(INR);

        Assert.assertTrue(currency.getSelectedCurrency().startsWith(INR), "Selected value was: " + currency.getSelectedCurrency());
        Assert.assertFalse(currency.isCurrencyListOpen(), "The list closes after selecting");
        Assert.assertFalse(currency.isCurrencyErrorDisplayed(), "No error after a valid selection");
    }

    @Test(groups = {"regression", "currency"}, description = "CUR-006 Select USD with the keyboard")
    @Story("Selection")
    @Severity(SeverityLevel.NORMAL)
    public void cur006_selectUsdWithKeyboard() {
        openCurrencySection();

        currency.selectCurrencyWithKeyboard(USD);

        Assert.assertTrue(currency.getSelectedCurrency().startsWith(USD), "Selected value was: " + currency.getSelectedCurrency());
        Assert.assertFalse(currency.isCurrencyListOpen(), "The list closes after selecting");
    }

    @Test(groups = {"regression", "currency"}, description = "CUR-007 Search filters, shows No options, clears on blur")
    @Story("Search")
    @Severity(SeverityLevel.NORMAL)
    public void cur007_searchFiltersAndUnmatchedTextIsClearedOnBlur() {
        openCurrencySection();

        currency.searchCurrency("us");
        Assert.assertTrue(currency.getCurrencyOptions().stream().anyMatch(o -> o.startsWith(USD + " (")),
                "Searching 'us' should list USD");

        currency.searchCurrency("zzz");
        currency.waitForNoCurrencyOptions();
        Assert.assertTrue(currency.hasNoCurrencyOptions(), "An unmatched search shows the no-options message");
        Assert.assertEquals(currency.getNoCurrencyOptionsText(), "No options");

        currency.blurCurrency();
        Assert.assertEquals(currency.getSelectedCurrency(), "", "Unmatched text is cleared on blur");
        Assert.assertFalse(currency.isCurrencyErrorDisplayed(), "Nothing was ever selected, so no error");
    }

    @Test(groups = {"regression", "currency"}, description = "CUR-008 Clear button shows the required error")
    @Story("Validation")
    @Severity(SeverityLevel.CRITICAL)
    public void cur008_clearButtonShowsRequiredError() {
        openCurrencySection();
        currency.selectCurrency(INR);

        currency.clearCurrency();

        Assert.assertEquals(currency.getCurrencyError(), REQUIRED);
        Assert.assertTrue(currency.isCurrencyErrorDisplayed(), "The error should be displayed");
        Assert.assertTrue(currency.isCurrencyMarkedInvalid(), "aria-invalid should be true");
        Assert.assertEquals(currency.getSelectedCurrency(), "");
    }

    @Test(groups = {"regression", "currency"}, description = "CUR-009 Keyboard clearing shows the required error")
    @Story("Validation")
    @Severity(SeverityLevel.NORMAL)
    public void cur009_keyboardClearShowsRequiredError() {
        openCurrencySection();
        currency.selectCurrency(INR);

        currency.clearCurrencyByKeyboard();

        Assert.assertEquals(currency.getCurrencyError(), REQUIRED);
        Assert.assertTrue(currency.isCurrencyMarkedInvalid(), "aria-invalid should be true");
        Assert.assertEquals(currency.getSelectedCurrency(), "");
    }

    @Test(groups = {"regression", "currency"}, description = "CUR-010 Error persists on blur and clears when a currency is selected")
    @Story("Validation")
    @Severity(SeverityLevel.NORMAL)
    public void cur010_errorPersistsOnBlurAndClearsOnSelection() {
        openCurrencySection();
        currency.selectCurrency(INR);
        currency.clearCurrency();
        Assert.assertEquals(currency.getCurrencyError(), REQUIRED, "Precondition: the required error is showing");

        currency.blurCurrency();
        Assert.assertTrue(currency.isCurrencyErrorDisplayed(), "The error remains after blur while the field is empty");

        currency.selectCurrency(USD);
        currency.waitForCurrencyErrorToDisappear();
        Assert.assertFalse(currency.isCurrencyErrorDisplayed(), "The error should disappear once a currency is selected");
        Assert.assertFalse(currency.isCurrencyMarkedInvalid(), "aria-invalid should be false again");
    }

    @Test(groups = {"regression", "currency"}, description = "CUR-011 Reopened list marks the selected currency")
    @Story("Dropdown")
    @Severity(SeverityLevel.NORMAL)
    public void cur011_reopenedListMarksSelectedCurrency() {
        openCurrencySection();
        currency.selectCurrency(INR);

        currency.openCurrencyList();

        Assert.assertTrue(currency.isCurrencyListOpen(), "The list should open again");
        Assert.assertTrue(currency.isCurrencyOptionSelected(INR), "INR should be marked selected");
        Assert.assertFalse(currency.isCurrencyOptionSelected(USD), "USD should not be marked selected");
    }

    @Test(groups = {"regression", "currency"}, description = "CUR-012 Escape closes only the list")
    @Story("Dropdown")
    @Severity(SeverityLevel.NORMAL)
    public void cur012_escapeClosesOnlyTheList() {
        openCurrencySection();
        currency.selectCurrency(INR);
        currency.openCurrencyList();

        currency.closeCurrencyListWithEscape();

        Assert.assertFalse(currency.isCurrencyListOpen(), "The list should close");
        Assert.assertTrue(currency.getSelectedCurrency().startsWith(INR), "INR should remain selected");
        Assert.assertTrue(dialog.isDialogDisplayed(), "The dialog should remain open");
    }

    @Test(groups = {"regression", "currency"}, description = "CUR-013 Selected currency persists across section switches")
    @Story("Section navigation")
    @Severity(SeverityLevel.CRITICAL)
    public void cur013_selectedCurrencyPersistsAcrossSectionSwitches() {
        openCurrencySection();
        currency.selectCurrency(INR);

        for (String other : List.of(CreateBusinessUnitDialog.BUSINESS_UNIT_DETAILS, "ERP Integration", "Payment Integration")) {
            dialog.selectSection(other);
            Assert.assertEquals(dialog.getSelectedSection(), other, "Selected row after leaving Currency for " + other);
            Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard on " + other);

            dialog.selectSection(CreateBusinessUnitCurrencySection.SECTION);
            Assert.assertEquals(dialog.getSelectedSection(), "Currency", "Selected row after returning from " + other);
            Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard on return");
            Assert.assertTrue(currency.getSelectedCurrency().startsWith(INR),
                    "INR should persist after visiting " + other + " but the field shows '" + currency.getSelectedCurrency() + "'");
        }
    }

    @Test(groups = {"regression", "currency"}, description = "CUR-014 Reopening the dialog resets Currency")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void cur014_reopeningTheDialogResetsCurrency() {
        openCurrencySection();
        currency.selectCurrency(USD);

        dialog.cancel();
        dashboard.clickAddBusinessUnit();
        CreateBusinessUnitDialog reopened = new CreateBusinessUnitDialog();
        reopened.waitForDialog();
        reopened.selectSection(CreateBusinessUnitCurrencySection.SECTION);

        Assert.assertEquals(currency.getSelectedCurrency(), "", "A reopened dialog starts with no currency");
        Assert.assertFalse(currency.isCurrencyErrorDisplayed(), "No error carries over");
        reopened.cancel();
    }

    @Test(groups = {"regression", "currency"}, description = "CUR-015 Cancelling with a currency creates nothing")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void cur015_cancellingWithACurrencyCreatesNothing() {
        dashboard = openDashboard();
        List<String> unitsBefore = dashboard.getBusinessUnitNames();
        dashboard.clickAddBusinessUnit();
        dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        dialog.selectSection(CreateBusinessUnitCurrencySection.SECTION);
        currency = new CreateBusinessUnitCurrencySection();
        currency.selectCurrency(INR);

        dialog.cancel();

        Assert.assertFalse(dialog.isDialogDisplayed(), "The dialog should be closed");
        Assert.assertEquals(dashboard.getBusinessUnitNames(), unitsBefore, "No Business Unit may have been created");
    }
}
