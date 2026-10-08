package ai.metayb.ui.onboarding.businessunit;

import ai.metayb.ui.components.onboarding.ErpField;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitDialog;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitErpSection;
import ai.metayb.ui.pages.onboarding.DashboardPage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.OnboardingAuthenticatedTest;

import java.util.List;

/**
 * ERP Integration section of the Create Business Unit dialog, covering only behavior verified on the live portal.
 * Every test opens the dialog itself from a fresh dashboard and finishes by leaving it. Nothing is created: Create is
 * never clicked, and every value entered is an obviously fake placeholder that only changes unsaved form state.
 *
 * Validation here is edit-driven and applies only while ERP Config Status is Active; the tests rely on editing a
 * field to empty to trigger it and never assume submit-time behavior. Whitespace-only and very long values are
 * deliberately not tested (recorded as observations only).
 */
@Epic("Onboarding Portal")
@Feature("Create Business Unit - ERP Integration")
public class ErpIntegrationSectionTest extends OnboardingAuthenticatedTest {

    private static final String FAKE_SECRET = "fake-not-a-secret";
    private static final String PROVIDER_REQUIRED = "ERP provider is required.";

    private DashboardPage dashboard;
    private CreateBusinessUnitDialog dialog;
    private CreateBusinessUnitErpSection erp;

    /** Opens the dialog from a fresh dashboard and shows the ERP Integration section. */
    private void openErpSection() {
        dashboard = openDashboard();
        dashboard.clickAddBusinessUnit();
        dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        dialog.selectSection(CreateBusinessUnitErpSection.SECTION);
        erp = new CreateBusinessUnitErpSection();
    }

    private static String fake(ErpField field) {
        return field.isSecret() ? FAKE_SECRET : "fake-" + field.name().toLowerCase().replace('_', '-');
    }

    private void assertAllFieldsEmpty() {
        for (ErpField field : ErpField.values()) {
            Assert.assertTrue(erp.isFieldEmpty(field), field + " should be empty");
        }
        Assert.assertEquals(erp.getProvider(), "", "Provider should be empty");
    }

    private void assertEveryFieldRequiredMarked(boolean expected) {
        for (ErpField field : ErpField.values()) {
            Assert.assertEquals(erp.isFieldRequiredMarked(field), expected,
                    "Required marker on '" + field.label() + "' (label shows '" + erp.getFieldLabel(field) + "')");
        }
        Assert.assertEquals(erp.isProviderRequiredMarked(), expected, "Required marker on ERP Provider");
    }

    // ------------------------------------------------------------------ section and defaults

    @Test(groups = {"regression", "erp"}, description = "ERP-001 ERP section opens correctly")
    @Story("Section")
    @Severity(SeverityLevel.CRITICAL)
    public void erp001_sectionOpensCorrectly() {
        openErpSection();

        Assert.assertEquals(dialog.getSelectedSection(), "ERP Integration", "Selected navigation row");
        Assert.assertEquals(erp.getErpHeading(), "ERP Integration");
        Assert.assertEquals(erp.getErpDescription(), "Configure the ERP system connection for this business unit.");
        Assert.assertTrue(erp.isErpSectionDisplayed(), "The ERP panel should be displayed");
        Assert.assertTrue(dialog.isSectionDisplayed("ERP Integration"), "ERP Integration should be the visible panel");
        Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard");
    }

    @Test(groups = {"regression", "erp"}, description = "ERP-002 Default state")
    @Story("Initial state")
    @Severity(SeverityLevel.NORMAL)
    public void erp002_defaultState() {
        openErpSection();

        Assert.assertEquals(erp.getConfigStatus(), "Inactive", "Default status");
        Assert.assertEquals(erp.getConfigStatusValue(), "inactive", "Default status value");
        assertAllFieldsEmpty();
        assertEveryFieldRequiredMarked(false);
        Assert.assertEquals(erp.getVisibleErrors(), List.of(), "No validation errors initially");
    }

    @Test(groups = {"regression", "erp"}, description = "ERP-003 Config Status options")
    @Story("ERP Config Status")
    @Severity(SeverityLevel.NORMAL)
    public void erp003_configStatusOptions() {
        openErpSection();

        erp.openConfigStatusList();

        Assert.assertEquals(erp.getConfigStatusOptions(), List.of("Active", "Inactive"), "Status options");
        Assert.assertTrue(erp.isConfigStatusOptionSelected("Inactive"), "Inactive should be marked selected");
        Assert.assertFalse(erp.isConfigStatusOptionSelected("Active"), "Active should not be marked selected");
        erp.closeConfigStatusListWithEscape();
        Assert.assertEquals(erp.getConfigStatus(), "Inactive", "Displayed value after viewing the options");
    }

    // ------------------------------------------------------------------ Active branch

    @Test(groups = {"regression", "erp"}, description = "ERP-004 Selecting Active marks every field required")
    @Story("Conditional validation")
    @Severity(SeverityLevel.CRITICAL)
    public void erp004_activeMarksEveryFieldRequired() {
        openErpSection();

        erp.selectConfigStatus("Active");

        Assert.assertEquals(erp.getConfigStatusValue(), "active");
        assertEveryFieldRequiredMarked(true);
        Assert.assertEquals(erp.getVisibleErrors(), List.of(), "Untouched fields must not show errors immediately");
    }

    @Test(groups = {"regression", "erp"}, description = "ERP-005 Untouched Active field shows no error on blur")
    @Story("Conditional validation")
    @Severity(SeverityLevel.NORMAL)
    public void erp005_untouchedActiveFieldShowsNoErrorOnBlur() {
        openErpSection();
        erp.selectConfigStatus("Active");

        erp.focusField(ErpField.DATABASE);
        erp.blurErp();

        Assert.assertFalse(erp.isFieldErrorDisplayed(ErpField.DATABASE), "Focus and blur alone must not validate");
        Assert.assertFalse(erp.isFieldMarkedInvalid(ErpField.DATABASE));
        Assert.assertEquals(erp.getVisibleErrors(), List.of());
    }

    @Test(groups = {"regression", "erp"}, description = "ERP-006 Exact required messages for every field")
    @Story("Conditional validation")
    @Severity(SeverityLevel.CRITICAL)
    public void erp006_requiredMessagesForEveryField() {
        openErpSection();
        erp.selectConfigStatus("Active");

        for (ErpField field : ErpField.values()) {
            erp.enterFieldValue(field, "a");
            erp.clearFieldValue(field);

            Assert.assertEquals(erp.getFieldError(field), field.requiredMessage(), "Error for " + field.label());
            Assert.assertTrue(erp.isFieldMarkedInvalid(field), field.label() + " should be aria-invalid");
            Assert.assertTrue(erp.isFieldDescribedByItsError(field), field.label() + " aria-describedby should point at its error");

            erp.enterFieldValue(field, fake(field));
            erp.waitForFieldErrorToDisappear(field);
            Assert.assertFalse(erp.isFieldMarkedInvalid(field), field.label() + " should be valid after a value is supplied");
        }

        erp.selectProvider("Fabric");
        erp.clearProvider();
        Assert.assertEquals(erp.getProviderError(), PROVIDER_REQUIRED, "Error for ERP Provider");
        Assert.assertTrue(erp.isProviderMarkedInvalid(), "ERP Provider should be aria-invalid");
        Assert.assertTrue(erp.isProviderDescribedByItsError(), "ERP Provider aria-describedby should point at its error");
        erp.selectProvider("Fabric");
        erp.waitForProviderErrorToDisappear();
        Assert.assertFalse(erp.isProviderMarkedInvalid(), "ERP Provider should be valid after selecting");
    }

    // ------------------------------------------------------------------ provider

    @Test(groups = {"regression", "erp"}, description = "ERP-007 Provider offers and selects Fabric")
    @Story("ERP Provider")
    @Severity(SeverityLevel.NORMAL)
    public void erp007_providerOffersAndSelectsFabric() {
        openErpSection();
        erp.selectConfigStatus("Active");

        erp.openProviderList();
        Assert.assertTrue(erp.getProviderOptions().contains("Fabric"), "Fabric should be offered");
        erp.selectProvider("Fabric");

        Assert.assertEquals(erp.getProvider(), "Fabric");
        Assert.assertFalse(erp.isProviderListOpen(), "The list closes after selecting");
        Assert.assertFalse(erp.isProviderErrorDisplayed(), "No error after selecting a provider");
    }

    @Test(groups = {"regression", "erp"}, description = "ERP-008 Provider search, No options and clearing")
    @Story("ERP Provider")
    @Severity(SeverityLevel.NORMAL)
    public void erp008_providerSearchAndClearing() {
        openErpSection();
        erp.selectConfigStatus("Active");
        erp.selectProvider("Fabric");
        erp.clearProvider();
        Assert.assertEquals(erp.getProviderError(), PROVIDER_REQUIRED, "Clearing the provider while Active is required");

        erp.searchProvider("Fabric");
        Assert.assertTrue(erp.getProviderOptions().contains("Fabric"), "Searching 'Fabric' should return Fabric");

        erp.searchProvider("zzz");
        erp.waitForNoProviderOptions();
        Assert.assertTrue(erp.hasNoProviderOptions(), "An unmatched search shows the no-options message");
        Assert.assertEquals(erp.getNoProviderOptionsText(), "No options");

        erp.blurErp();
        Assert.assertEquals(erp.getProvider(), "", "Unmatched text is cleared on blur");
        Assert.assertTrue(erp.isProviderErrorDisplayed(), "The required error remains while the provider is empty");
    }

    // ------------------------------------------------------------------ Active / Inactive

    @Test(groups = {"regression", "erp"}, description = "ERP-009 Active to Inactive keeps values and drops validation")
    @Story("Conditional validation")
    @Severity(SeverityLevel.CRITICAL)
    public void erp009_activeInactiveSwitching() {
        openErpSection();
        erp.enterFieldValue(ErpField.SQL_ENDPOINT, fake(ErpField.SQL_ENDPOINT));
        erp.enterFieldValue(ErpField.DATABASE, fake(ErpField.DATABASE));
        erp.enterFieldValue(ErpField.CLIENT_ID, fake(ErpField.CLIENT_ID));

        erp.selectConfigStatus("Active");
        assertEveryFieldRequiredMarked(true);

        erp.clearFieldValue(ErpField.DATABASE);
        Assert.assertEquals(erp.getFieldError(ErpField.DATABASE), ErpField.DATABASE.requiredMessage(),
                "Precondition: an error is showing while Active");

        erp.selectConfigStatus("Inactive");
        assertEveryFieldRequiredMarked(false);
        Assert.assertEquals(erp.getVisibleErrors(), List.of(), "Errors disappear when the status becomes Inactive");
        Assert.assertEquals(erp.getFieldValue(ErpField.SQL_ENDPOINT), fake(ErpField.SQL_ENDPOINT), "Values are retained");
        Assert.assertEquals(erp.getFieldValue(ErpField.CLIENT_ID), fake(ErpField.CLIENT_ID), "Values are retained");

        erp.clearFieldValue(ErpField.CLIENT_ID);
        Assert.assertFalse(erp.isFieldErrorDisplayed(ErpField.CLIENT_ID), "Clearing while Inactive shows no required error");
        Assert.assertFalse(erp.isFieldRequiredMarked(ErpField.CLIENT_ID), "No required marker while Inactive");
        Assert.assertFalse(erp.isFieldMarkedInvalid(ErpField.CLIENT_ID));

        erp.selectConfigStatus("Active");
        Assert.assertTrue(erp.isFieldRequiredMarked(ErpField.CLIENT_ID), "The cleared field is marked required again");
        Assert.assertFalse(erp.isFieldErrorDisplayed(ErpField.CLIENT_ID),
                "Validation is edit-driven, so there is no immediate error after switching back to Active");
    }

    @Test(groups = {"regression", "erp"}, description = "ERP-010 Status select opens with ArrowDown and closes with Escape")
    @Story("ERP Config Status")
    @Severity(SeverityLevel.NORMAL)
    public void erp010_statusKeyboardBehavior() {
        openErpSection();

        erp.openConfigStatusListWithKeyboard();
        Assert.assertTrue(erp.isConfigStatusListOpen(), "ArrowDown should open the status list");

        erp.closeConfigStatusListWithEscape();

        Assert.assertFalse(erp.isConfigStatusListOpen(), "Escape should close the list");
        Assert.assertEquals(erp.getConfigStatus(), "Inactive", "The selected value is unchanged");
        Assert.assertTrue(dialog.isDialogDisplayed(), "The dialog should remain open");
    }

    // ------------------------------------------------------------------ secret

    @Test(groups = {"regression", "erp"}, description = "ERP-011 MS Client Secret is a masked password field")
    @Story("MS Client Secret")
    @Severity(SeverityLevel.NORMAL)
    @Description("Uses typeSecret with an obviously fake value; the value is never read back, asserted or logged.")
    public void erp011_clientSecretIsMasked() {
        openErpSection();

        erp.enterFieldValue(ErpField.CLIENT_SECRET, FAKE_SECRET);

        Assert.assertEquals(erp.getFieldInputType(ErpField.CLIENT_SECRET), "password", "The field must stay a password input");
        Assert.assertFalse(erp.isFieldEmpty(ErpField.CLIENT_SECRET), "The field should hold the entered value");
    }

    // ------------------------------------------------------------------ navigation and closing

    @Test(groups = {"regression", "erp"}, description = "ERP-012 ERP state persists across section switches")
    @Story("Section navigation")
    @Severity(SeverityLevel.CRITICAL)
    public void erp012_stateSurvivesSectionSwitches() {
        openErpSection();
        erp.selectConfigStatus("Active");
        erp.selectProvider("Fabric");
        erp.enterFieldValue(ErpField.SQL_ENDPOINT, fake(ErpField.SQL_ENDPOINT));
        erp.enterFieldValue(ErpField.CLIENT_ID, fake(ErpField.CLIENT_ID));

        for (String other : List.of(CreateBusinessUnitDialog.BUSINESS_UNIT_DETAILS, "Currency", "Payment Integration")) {
            dialog.selectSection(other);
            Assert.assertEquals(dialog.getSelectedSection(), other, "Selected row after leaving ERP for " + other);
            Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard on " + other);

            dialog.selectSection(CreateBusinessUnitErpSection.SECTION);
            Assert.assertEquals(dialog.getSelectedSection(), "ERP Integration", "Selected row after returning from " + other);
            Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard on return");
            Assert.assertEquals(erp.getConfigStatus(), "Active", "Status after visiting " + other);
            Assert.assertEquals(erp.getProvider(), "Fabric", "Provider after visiting " + other);
            Assert.assertEquals(erp.getFieldValue(ErpField.SQL_ENDPOINT), fake(ErpField.SQL_ENDPOINT), "SQL Endpoint after visiting " + other);
            Assert.assertEquals(erp.getFieldValue(ErpField.CLIENT_ID), fake(ErpField.CLIENT_ID), "Client ID after visiting " + other);
        }
    }

    @Test(groups = {"regression", "erp"}, description = "ERP-013 Reopening the dialog resets ERP")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void erp013_reopeningResetsErp() {
        openErpSection();
        erp.selectConfigStatus("Active");
        erp.selectProvider("Fabric");
        erp.enterFieldValue(ErpField.SQL_ENDPOINT, fake(ErpField.SQL_ENDPOINT));

        dialog.cancel();
        dashboard.clickAddBusinessUnit();
        CreateBusinessUnitDialog reopened = new CreateBusinessUnitDialog();
        reopened.waitForDialog();
        reopened.selectSection(CreateBusinessUnitErpSection.SECTION);

        Assert.assertEquals(erp.getConfigStatus(), "Inactive", "Status resets");
        assertAllFieldsEmpty();
        assertEveryFieldRequiredMarked(false);
        Assert.assertEquals(erp.getVisibleErrors(), List.of(), "No errors carry over");
        reopened.cancel();
    }

    @Test(groups = {"regression", "erp"}, description = "ERP-014 Cancelling with ERP values creates nothing")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void erp014_cancellingCreatesNothing() {
        dashboard = openDashboard();
        List<String> unitsBefore = dashboard.getBusinessUnitNames();
        dashboard.clickAddBusinessUnit();
        dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        dialog.selectSection(CreateBusinessUnitErpSection.SECTION);
        erp = new CreateBusinessUnitErpSection();
        erp.selectConfigStatus("Active");
        erp.selectProvider("Fabric");
        erp.enterFieldValue(ErpField.SQL_ENDPOINT, fake(ErpField.SQL_ENDPOINT));
        erp.enterFieldValue(ErpField.DATABASE, fake(ErpField.DATABASE));

        dialog.cancel();

        Assert.assertFalse(dialog.isDialogDisplayed(), "The dialog should be closed");
        Assert.assertEquals(dashboard.getBusinessUnitNames(), unitsBefore, "No Business Unit may have been created");
    }
}
