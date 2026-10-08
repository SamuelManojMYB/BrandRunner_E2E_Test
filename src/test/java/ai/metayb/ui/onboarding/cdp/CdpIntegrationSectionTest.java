package ai.metayb.ui.onboarding.cdp;

import ai.metayb.ui.pages.onboarding.CreateBusinessUnitCdpSection;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitDialog;
import ai.metayb.ui.pages.onboarding.DashboardPage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.OnboardingAuthenticatedTest;

import java.util.List;

/**
 * CDP Integration section of the Create Business Unit dialog, covering only behavior verified on the live portal.
 * Every test opens the dialog itself from a fresh dashboard and finishes by leaving it. Nothing is created: Create is
 * never clicked, and every value entered is an obviously fake placeholder that only changes unsaved form state.
 *
 * Validation here is edit-driven and applies only while CDP Config Status is Active; the tests rely on editing a
 * field to empty to trigger it and never assume submit-time behavior. The country list is only known to contain Ghana,
 * so neither its full content nor its order is asserted. Whitespace-only and very long values are deliberately not
 * tested, and the "Unsaved changes" chip and nav-row dot are not automated.
 */
@Epic("Onboarding Portal")
@Feature("Create Business Unit - CDP Integration")
public class CdpIntegrationSectionTest extends OnboardingAuthenticatedTest {

    private static final String FAKE_COMPANY = "cdp-company-test-value";
    private static final String COMPANY_REQUIRED = "CDP company name is required.";
    private static final String COUNTRY_REQUIRED = "Country is required.";

    private DashboardPage dashboard;
    private CreateBusinessUnitDialog dialog;
    private CreateBusinessUnitCdpSection cdp;

    /** Opens the dialog from a fresh dashboard and shows the CDP Integration section. */
    private void openCdpSection() {
        dashboard = openDashboard();
        dashboard.clickAddBusinessUnit();
        dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        dialog.selectSection(CreateBusinessUnitCdpSection.SECTION);
        cdp = new CreateBusinessUnitCdpSection();
    }

    private void assertRequiredMarked(boolean expected) {
        Assert.assertEquals(cdp.isCompanyNameRequiredMarked(), expected,
                "Required marker on CDP Company Name (label shows '" + cdp.getCompanyNameLabel() + "')");
        Assert.assertEquals(cdp.isCountryRequiredMarked(), expected, "Required marker on Country");
    }

    private void assertDefaultState() {
        Assert.assertEquals(cdp.getConfigStatus(), "Inactive", "Default status");
        Assert.assertEquals(cdp.getConfigStatusValue(), "inactive", "Default status value");
        Assert.assertTrue(cdp.isCompanyNameEmpty(), "Company name should be empty");
        Assert.assertEquals(cdp.getCountry(), "", "Country should be empty");
        assertRequiredMarked(false);
        Assert.assertEquals(cdp.getVisibleErrors(), List.of(), "No validation errors");
    }

    // ------------------------------------------------------------------ section and defaults

    @Test(groups = {"regression", "cdp"}, description = "CDP-001 CDP section opens correctly")
    @Story("Section")
    @Severity(SeverityLevel.CRITICAL)
    public void cdp001_sectionOpensCorrectly() {
        openCdpSection();

        Assert.assertEquals(dialog.getSelectedSection(), "CDP Integration", "Selected navigation row");
        Assert.assertEquals(cdp.getCdpHeading(), "CDP Integration");
        Assert.assertEquals(cdp.getCdpDescription(), "Configure the CDP connection for this business unit.");
        Assert.assertTrue(cdp.isCdpSectionDisplayed(), "The CDP panel should be displayed");
        Assert.assertTrue(dialog.isSectionDisplayed("CDP Integration"), "CDP Integration should be the visible panel");
        Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard");
    }

    @Test(groups = {"regression", "cdp"}, description = "CDP-002 Default state")
    @Story("Initial state")
    @Severity(SeverityLevel.NORMAL)
    public void cdp002_defaultState() {
        openCdpSection();

        assertDefaultState();
    }

    @Test(groups = {"regression", "cdp"}, description = "CDP-003 Config Status options")
    @Story("CDP Config Status")
    @Severity(SeverityLevel.NORMAL)
    public void cdp003_configStatusOptions() {
        openCdpSection();

        cdp.openConfigStatusList();

        Assert.assertEquals(cdp.getConfigStatusOptions(), List.of("Active", "Inactive"), "Status options");
        Assert.assertTrue(cdp.isConfigStatusOptionSelected("Inactive"), "Inactive should be marked selected");
        Assert.assertFalse(cdp.isConfigStatusOptionSelected("Active"), "Active should not be marked selected");
        cdp.closeConfigStatusListWithEscape();
        Assert.assertEquals(cdp.getConfigStatus(), "Inactive", "Displayed value after viewing the options");
    }

    // ------------------------------------------------------------------ Active branch

    @Test(groups = {"regression", "cdp"}, description = "CDP-004 Selecting Active marks company name and country required")
    @Story("Conditional validation")
    @Severity(SeverityLevel.CRITICAL)
    public void cdp004_activeMarksFieldsRequired() {
        openCdpSection();

        cdp.selectConfigStatus("Active");

        Assert.assertEquals(cdp.getConfigStatusValue(), "active");
        assertRequiredMarked(true);
        Assert.assertEquals(cdp.getVisibleErrors(), List.of(), "Untouched fields must not show errors immediately");
    }

    @Test(groups = {"regression", "cdp"}, description = "CDP-005 Untouched Active field shows no error on blur")
    @Story("Conditional validation")
    @Severity(SeverityLevel.NORMAL)
    public void cdp005_untouchedActiveFieldShowsNoErrorOnBlur() {
        openCdpSection();
        cdp.selectConfigStatus("Active");

        cdp.focusCompanyName();
        cdp.blurCdp();

        Assert.assertFalse(cdp.isCompanyNameErrorDisplayed(), "Focus and blur alone must not validate");
        Assert.assertFalse(cdp.isCompanyNameMarkedInvalid());
        Assert.assertEquals(cdp.getVisibleErrors(), List.of());
    }

    @Test(groups = {"regression", "cdp"}, description = "CDP-006 Company name required validation")
    @Story("CDP Company Name")
    @Severity(SeverityLevel.CRITICAL)
    public void cdp006_companyNameRequiredValidation() {
        openCdpSection();
        cdp.selectConfigStatus("Active");

        cdp.enterCompanyName(FAKE_COMPANY);
        cdp.clearCompanyName();

        Assert.assertEquals(cdp.getCompanyNameError(), COMPANY_REQUIRED, "Error for CDP Company Name");
        Assert.assertTrue(cdp.isCompanyNameMarkedInvalid(), "CDP Company Name should be aria-invalid");
        Assert.assertTrue(cdp.isCompanyNameDescribedByItsError(), "aria-describedby should point at its error");

        cdp.enterCompanyName(FAKE_COMPANY);
        cdp.waitForCompanyNameErrorToDisappear();
        Assert.assertFalse(cdp.isCompanyNameMarkedInvalid(), "The company name should be valid once a value is supplied");
        Assert.assertEquals(cdp.getVisibleErrors(), List.of());
    }

    @Test(groups = {"regression", "cdp"}, description = "CDP-007 Country required validation")
    @Story("Country")
    @Severity(SeverityLevel.CRITICAL)
    public void cdp007_countryRequiredValidation() {
        openCdpSection();
        cdp.selectConfigStatus("Active");
        cdp.selectCountry(CreateBusinessUnitCdpSection.GHANA);

        cdp.clearCountryWithButton();

        Assert.assertEquals(cdp.getCountryError(), COUNTRY_REQUIRED, "Error after clearing with the Clear button");
        Assert.assertTrue(cdp.isCountryMarkedInvalid(), "Country should be aria-invalid");
        Assert.assertTrue(cdp.isCountryDescribedByItsError(), "Country aria-describedby should point at its error");

        cdp.selectCountry(CreateBusinessUnitCdpSection.GHANA);
        cdp.waitForCountryErrorToDisappear();
        Assert.assertFalse(cdp.isCountryMarkedInvalid(), "Country should be valid after selecting");

        cdp.clearCountryWithKeyboard();

        Assert.assertEquals(cdp.getCountryError(), COUNTRY_REQUIRED, "Error after clearing with the keyboard");
        Assert.assertTrue(cdp.isCountryMarkedInvalid());
        Assert.assertTrue(cdp.isCountryDescribedByItsError());

        cdp.selectCountry(CreateBusinessUnitCdpSection.GHANA);
        cdp.waitForCountryErrorToDisappear();
        Assert.assertFalse(cdp.isCountryErrorDisplayed(), "A valid selection clears the error");
    }

    // ------------------------------------------------------------------ country

    @Test(groups = {"regression", "cdp"}, description = "CDP-008 Country offers and selects Ghana")
    @Story("Country")
    @Severity(SeverityLevel.NORMAL)
    public void cdp008_countryOffersAndSelectsGhana() {
        openCdpSection();

        cdp.openCountryList();
        Assert.assertTrue(cdp.getCountryOptions().contains("Ghana"), "Ghana should be offered");
        cdp.selectCountry(CreateBusinessUnitCdpSection.GHANA);

        Assert.assertEquals(cdp.getCountry(), "Ghana");
        Assert.assertFalse(cdp.isCountryListOpen(), "The list closes after selecting");
        Assert.assertEquals(cdp.getConfigStatus(), "Inactive", "Status is unchanged by choosing a country");
        Assert.assertTrue(cdp.isCompanyNameEmpty(), "The company name is unchanged by choosing a country");
        assertRequiredMarked(false);
        Assert.assertEquals(cdp.getVisibleErrors(), List.of(), "No error after selecting a country");
    }

    @Test(groups = {"regression", "cdp"}, description = "CDP-009 Country search, No options and blur")
    @Story("Country")
    @Severity(SeverityLevel.NORMAL)
    public void cdp009_countrySearchAndBlur() {
        openCdpSection();

        cdp.searchCountry("ri");
        List<String> matches = cdp.getCountryOptions();
        Assert.assertFalse(matches.isEmpty(), "Searching 'ri' should return at least one country");
        for (String match : matches) {
            Assert.assertTrue(match.toLowerCase().contains("ri"), "'" + match + "' should contain the searched text");
        }

        cdp.searchCountry("zzz");
        cdp.waitForNoCountryOptions();
        Assert.assertTrue(cdp.hasNoCountryOptions(), "An unmatched search shows the no-options message");
        Assert.assertEquals(cdp.getNoCountryOptionsText(), "No options");

        cdp.blurCdp();
        Assert.assertEquals(cdp.getCountry(), "", "Unmatched text is cleared on blur");
        Assert.assertEquals(cdp.getVisibleErrors(), List.of(), "No error while Inactive");
    }

    // ------------------------------------------------------------------ Active / Inactive

    @Test(groups = {"regression", "cdp"}, description = "CDP-010 Active to Inactive keeps values and drops validation")
    @Story("Conditional validation")
    @Severity(SeverityLevel.CRITICAL)
    public void cdp010_activeInactiveSwitching() {
        openCdpSection();
        cdp.enterCompanyName(FAKE_COMPANY);
        cdp.selectCountry(CreateBusinessUnitCdpSection.GHANA);

        cdp.selectConfigStatus("Active");
        assertRequiredMarked(true);
        Assert.assertEquals(cdp.getVisibleErrors(), List.of(), "Switching to Active shows no immediate errors");

        cdp.clearCompanyName();
        Assert.assertEquals(cdp.getCompanyNameError(), COMPANY_REQUIRED, "Precondition: an error is showing while Active");

        cdp.selectConfigStatus("Inactive");
        assertRequiredMarked(false);
        Assert.assertEquals(cdp.getVisibleErrors(), List.of(), "Errors disappear when the status becomes Inactive");
        Assert.assertFalse(cdp.isCompanyNameMarkedInvalid(), "The company name is no longer invalid");
        Assert.assertTrue(cdp.isCompanyNameEmpty(), "The emptied company name stays empty");
        Assert.assertEquals(cdp.getCountry(), "Ghana", "The country value is retained");

        cdp.enterCompanyName(FAKE_COMPANY);
        cdp.clearCountryWithKeyboard();
        Assert.assertFalse(cdp.isCountryErrorDisplayed(), "Clearing while Inactive shows no required error");
        Assert.assertFalse(cdp.isCountryMarkedInvalid());
        Assert.assertFalse(cdp.isCountryRequiredMarked(), "No required marker while Inactive");

        cdp.selectConfigStatus("Active");
        assertRequiredMarked(true);
        Assert.assertEquals(cdp.getCompanyName(), FAKE_COMPANY, "The retained company name survives the switch");
        Assert.assertEquals(cdp.getCountry(), "", "The emptied country stays empty");
        Assert.assertFalse(cdp.isCountryErrorDisplayed(), "No immediate country error after switching back to Active");
        Assert.assertFalse(cdp.isCompanyNameErrorDisplayed(), "No immediate company name error after switching back to Active");
        Assert.assertEquals(cdp.getVisibleErrors(), List.of());
    }

    // ------------------------------------------------------------------ keyboard

    @Test(groups = {"regression", "cdp"}, description = "CDP-011 Keyboard behavior of the status and country lists")
    @Story("Keyboard")
    @Severity(SeverityLevel.NORMAL)
    public void cdp011_keyboardBehavior() {
        openCdpSection();

        cdp.openConfigStatusListWithKeyboard();
        Assert.assertTrue(cdp.isConfigStatusListOpen(), "ArrowDown should open the status list");
        cdp.closeConfigStatusListWithEscape();
        Assert.assertFalse(cdp.isConfigStatusListOpen(), "Escape should close the status list");
        Assert.assertEquals(cdp.getConfigStatus(), "Inactive", "The selected status is unchanged");
        Assert.assertTrue(dialog.isDialogDisplayed(), "The dialog should remain open");

        cdp.openCountryListWithKeyboard();
        Assert.assertTrue(cdp.isCountryListOpen(), "ArrowDown should open the country list");
        List<String> offered = cdp.getCountryOptions();
        cdp.selectHighlightedCountryWithKeyboard();
        String chosen = cdp.getCountry();
        Assert.assertFalse(chosen.isEmpty(), "ArrowDown then Enter should select an option");
        Assert.assertTrue(offered.contains(chosen), "The chosen country '" + chosen + "' should be one that was offered");

        cdp.openCountryListWithKeyboard();
        Assert.assertTrue(cdp.isCountryListOpen());
        cdp.closeCountryListWithEscape();
        Assert.assertFalse(cdp.isCountryListOpen(), "Escape should close the country list");
        Assert.assertEquals(cdp.getCountry(), chosen, "Escape keeps the selected country");
        Assert.assertTrue(dialog.isDialogDisplayed(), "Escape in the country list must not close the dialog");
    }

    // ------------------------------------------------------------------ navigation and closing

    @Test(groups = {"regression", "cdp"}, description = "CDP-012 CDP state persists across section switches")
    @Story("Section navigation")
    @Severity(SeverityLevel.CRITICAL)
    public void cdp012_stateSurvivesSectionSwitches() {
        openCdpSection();
        cdp.selectConfigStatus("Active");
        cdp.enterCompanyName(FAKE_COMPANY);
        cdp.selectCountry(CreateBusinessUnitCdpSection.GHANA);

        for (String other : List.of(CreateBusinessUnitDialog.BUSINESS_UNIT_DETAILS, "Currency", "ERP Integration",
                "Payment Integration")) {
            dialog.selectSection(other);
            Assert.assertEquals(dialog.getSelectedSection(), other, "Selected row after leaving CDP for " + other);
            Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard on " + other);

            dialog.selectSection(CreateBusinessUnitCdpSection.SECTION);
            Assert.assertEquals(dialog.getSelectedSection(), "CDP Integration", "Selected row after returning from " + other);
            Assert.assertTrue(dialog.isSectionDisplayed("CDP Integration"), "The CDP panel is visible again after " + other);
            Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard on return");
            Assert.assertEquals(cdp.getConfigStatus(), "Active", "Status after visiting " + other);
            Assert.assertEquals(cdp.getCompanyName(), FAKE_COMPANY, "Company name after visiting " + other);
            Assert.assertEquals(cdp.getCountry(), "Ghana", "Country after visiting " + other);
        }
    }

    @Test(groups = {"regression", "cdp"}, description = "CDP-013 Reopening the dialog resets CDP")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void cdp013_reopeningResetsCdp() {
        openCdpSection();
        cdp.selectConfigStatus("Active");
        cdp.enterCompanyName(FAKE_COMPANY);
        cdp.selectCountry(CreateBusinessUnitCdpSection.GHANA);

        dialog.cancel();
        Assert.assertFalse(dialog.isDialogDisplayed(), "Cancel closes the dialog");
        dashboard.clickAddBusinessUnit();
        CreateBusinessUnitDialog reopened = new CreateBusinessUnitDialog();
        reopened.waitForDialog();
        reopened.selectSection(CreateBusinessUnitCdpSection.SECTION);

        assertDefaultState();
        reopened.cancel();
    }

    @Test(groups = {"regression", "cdp"}, description = "CDP-014 Cancelling with CDP values creates nothing")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void cdp014_cancellingCreatesNothing() {
        dashboard = openDashboard();
        List<String> unitsBefore = dashboard.getBusinessUnitNames();
        dashboard.clickAddBusinessUnit();
        dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        dialog.selectSection(CreateBusinessUnitCdpSection.SECTION);
        cdp = new CreateBusinessUnitCdpSection();
        cdp.selectConfigStatus("Active");
        cdp.enterCompanyName(FAKE_COMPANY);
        cdp.selectCountry(CreateBusinessUnitCdpSection.GHANA);

        dialog.cancel();

        Assert.assertFalse(dialog.isDialogDisplayed(), "The dialog should be closed");
        Assert.assertEquals(dashboard.getBusinessUnitNames(), unitsBefore, "No Business Unit may have been created");
    }
}
