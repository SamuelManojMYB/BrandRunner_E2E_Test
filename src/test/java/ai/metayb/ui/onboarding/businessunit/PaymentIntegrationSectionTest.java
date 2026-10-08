package ai.metayb.ui.onboarding.businessunit;

import ai.metayb.ui.pages.onboarding.CreateBusinessUnitDialog;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitPaymentSection;
import ai.metayb.ui.pages.onboarding.DashboardPage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.OnboardingAuthenticatedTest;

import java.util.List;

/**
 * Payment Integration section of the Create Business Unit dialog, covering only behavior verified on the live portal.
 * Every test opens the dialog itself from a fresh dashboard and finishes by leaving it. Nothing is created: Create is
 * never clicked, and the only secret ever entered is an obviously fake placeholder that changes unsaved form state.
 *
 * Validation here is edit-driven and applies only while Payment Config Status is Active; the tests rely on editing a
 * field to empty to trigger it and never assume submit-time behavior. The secret's value is never read, asserted or
 * logged: only whether it is empty and its input type are checked. Whitespace-only and very long values are
 * deliberately not tested, and the "Unsaved changes" chip and nav-row dot are not automated.
 */
@Epic("Onboarding Portal")
@Feature("Create Business Unit - Payment Integration")
public class PaymentIntegrationSectionTest extends OnboardingAuthenticatedTest {

    private static final String FAKE_SECRET = "payment-secret-test-value";
    private static final String PROVIDER_REQUIRED = "Payment provider is required.";
    private static final String SECRET_REQUIRED = "PAYSTACK secret key is required.";

    private DashboardPage dashboard;
    private CreateBusinessUnitDialog dialog;
    private CreateBusinessUnitPaymentSection payment;

    /** Opens the dialog from a fresh dashboard and shows the Payment Integration section. */
    private void openPaymentSection() {
        dashboard = openDashboard();
        dashboard.clickAddBusinessUnit();
        dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        dialog.selectSection(CreateBusinessUnitPaymentSection.SECTION);
        payment = new CreateBusinessUnitPaymentSection();
    }

    private void assertRequiredMarked(boolean expected) {
        Assert.assertEquals(payment.isProviderRequiredMarked(), expected, "Required marker on Payment Provider");
        Assert.assertEquals(payment.isSecretKeyRequiredMarked(), expected,
                "Required marker on PAYSTACK Secret Key (label shows '" + payment.getSecretKeyLabel() + "')");
    }

    private void assertDefaultState() {
        Assert.assertEquals(payment.getConfigStatus(), "Inactive", "Default status");
        Assert.assertEquals(payment.getConfigStatusValue(), "inactive", "Default status value");
        Assert.assertEquals(payment.getProvider(), "", "Provider should be empty");
        Assert.assertTrue(payment.isSecretKeyEmpty(), "Secret should be empty");
        Assert.assertEquals(payment.getSecretKeyInputType(), "password", "The secret is masked by default");
        assertRequiredMarked(false);
        Assert.assertEquals(payment.getVisibleErrors(), List.of(), "No validation errors");
    }

    // ------------------------------------------------------------------ section and defaults

    @Test(groups = {"regression", "payment"}, description = "PAY-001 Payment section opens correctly")
    @Story("Section")
    @Severity(SeverityLevel.CRITICAL)
    public void pay001_sectionOpensCorrectly() {
        openPaymentSection();

        Assert.assertEquals(dialog.getSelectedSection(), "Payment Integration", "Selected navigation row");
        Assert.assertEquals(payment.getPaymentHeading(), "Payment Integration");
        Assert.assertEquals(payment.getPaymentDescription(), "Configure the payment provider connection for this business unit.");
        Assert.assertTrue(payment.isPaymentSectionDisplayed(), "The Payment panel should be displayed");
        Assert.assertTrue(dialog.isSectionDisplayed("Payment Integration"), "Payment Integration should be the visible panel");
        Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard");
    }

    @Test(groups = {"regression", "payment"}, description = "PAY-002 Default state")
    @Story("Initial state")
    @Severity(SeverityLevel.NORMAL)
    public void pay002_defaultState() {
        openPaymentSection();

        assertDefaultState();
    }

    @Test(groups = {"regression", "payment"}, description = "PAY-003 Config Status options")
    @Story("Payment Config Status")
    @Severity(SeverityLevel.NORMAL)
    public void pay003_configStatusOptions() {
        openPaymentSection();

        payment.openConfigStatusList();

        Assert.assertEquals(payment.getConfigStatusOptions(), List.of("Active", "Inactive"), "Status options");
        Assert.assertTrue(payment.isConfigStatusOptionSelected("Inactive"), "Inactive should be marked selected");
        Assert.assertFalse(payment.isConfigStatusOptionSelected("Active"), "Active should not be marked selected");
        payment.closeConfigStatusListWithEscape();
        Assert.assertEquals(payment.getConfigStatus(), "Inactive", "Displayed value after viewing the options");
    }

    // ------------------------------------------------------------------ Active branch

    @Test(groups = {"regression", "payment"}, description = "PAY-004 Selecting Active marks provider and secret required")
    @Story("Conditional validation")
    @Severity(SeverityLevel.CRITICAL)
    public void pay004_activeMarksFieldsRequired() {
        openPaymentSection();

        payment.selectConfigStatus("Active");

        Assert.assertEquals(payment.getConfigStatusValue(), "active");
        assertRequiredMarked(true);
        Assert.assertEquals(payment.getVisibleErrors(), List.of(), "Untouched fields must not show errors immediately");
    }

    @Test(groups = {"regression", "payment"}, description = "PAY-005 Untouched Active field shows no error on blur")
    @Story("Conditional validation")
    @Severity(SeverityLevel.NORMAL)
    public void pay005_untouchedActiveFieldShowsNoErrorOnBlur() {
        openPaymentSection();
        payment.selectConfigStatus("Active");

        payment.focusSecretKey();
        payment.blurPayment();

        Assert.assertFalse(payment.isSecretKeyErrorDisplayed(), "Focus and blur alone must not validate");
        Assert.assertFalse(payment.isSecretKeyMarkedInvalid());
        Assert.assertEquals(payment.getVisibleErrors(), List.of());
    }

    @Test(groups = {"regression", "payment"}, description = "PAY-006 Provider required validation")
    @Story("Payment Provider")
    @Severity(SeverityLevel.CRITICAL)
    public void pay006_providerRequiredValidation() {
        openPaymentSection();
        payment.selectConfigStatus("Active");
        payment.selectProvider(CreateBusinessUnitPaymentSection.PAYSTACK);

        payment.clearProviderWithButton();

        Assert.assertEquals(payment.getProviderError(), PROVIDER_REQUIRED, "Error after clearing with the Clear button");
        Assert.assertTrue(payment.isProviderMarkedInvalid(), "Payment Provider should be aria-invalid");
        Assert.assertTrue(payment.isProviderDescribedByItsError(), "Payment Provider aria-describedby should point at its error");

        payment.selectProvider(CreateBusinessUnitPaymentSection.PAYSTACK);
        payment.waitForProviderErrorToDisappear();
        Assert.assertFalse(payment.isProviderMarkedInvalid(), "Payment Provider should be valid after selecting");

        payment.clearProviderWithKeyboard();

        Assert.assertEquals(payment.getProviderError(), PROVIDER_REQUIRED, "Error after clearing with the keyboard");
        Assert.assertTrue(payment.isProviderMarkedInvalid());

        payment.selectProvider(CreateBusinessUnitPaymentSection.PAYSTACK);
        payment.waitForProviderErrorToDisappear();
        Assert.assertFalse(payment.isProviderErrorDisplayed(), "A valid selection clears the error");
    }

    @Test(groups = {"regression", "payment"}, description = "PAY-007 Secret required validation")
    @Story("PAYSTACK Secret Key")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Uses typeSecret with an obviously fake value; the value is never read back, asserted or logged.")
    public void pay007_secretRequiredValidation() {
        openPaymentSection();
        payment.selectConfigStatus("Active");

        payment.enterSecretKey(FAKE_SECRET);
        payment.clearSecretKey();

        Assert.assertEquals(payment.getSecretKeyError(), SECRET_REQUIRED, "Error for PAYSTACK Secret Key");
        Assert.assertTrue(payment.isSecretKeyMarkedInvalid(), "PAYSTACK Secret Key should be aria-invalid");
        Assert.assertTrue(payment.isSecretKeyDescribedByItsError(), "aria-describedby should point at its error");

        payment.enterSecretKey(FAKE_SECRET);
        payment.waitForSecretKeyErrorToDisappear();
        Assert.assertFalse(payment.isSecretKeyMarkedInvalid(), "The secret should be valid once a value is supplied");
        Assert.assertEquals(payment.getVisibleErrors(), List.of());
    }

    // ------------------------------------------------------------------ provider

    @Test(groups = {"regression", "payment"}, description = "PAY-008 Provider offers and selects Paystack")
    @Story("Payment Provider")
    @Severity(SeverityLevel.NORMAL)
    public void pay008_providerOffersAndSelectsPaystack() {
        openPaymentSection();

        payment.openProviderList();
        Assert.assertTrue(payment.getProviderOptions().contains("Paystack"), "Paystack should be offered");
        payment.selectProvider(CreateBusinessUnitPaymentSection.PAYSTACK);

        Assert.assertEquals(payment.getProvider(), "Paystack");
        Assert.assertFalse(payment.isProviderListOpen(), "The list closes after selecting");
        Assert.assertEquals(payment.getConfigStatus(), "Inactive", "Status is unchanged by choosing a provider");
        Assert.assertTrue(payment.isSecretKeyEmpty(), "The secret is unchanged by choosing a provider");
        Assert.assertEquals(payment.getSecretKeyInputType(), "password");
        assertRequiredMarked(false);
        Assert.assertEquals(payment.getVisibleErrors(), List.of(), "No error after selecting a provider");
    }

    @Test(groups = {"regression", "payment"}, description = "PAY-009 Provider search, No options and blur")
    @Story("Payment Provider")
    @Severity(SeverityLevel.NORMAL)
    public void pay009_providerSearchAndBlur() {
        openPaymentSection();
        payment.selectConfigStatus("Active");
        payment.selectProvider(CreateBusinessUnitPaymentSection.PAYSTACK);
        payment.clearProviderWithKeyboard();
        Assert.assertEquals(payment.getProviderError(), PROVIDER_REQUIRED, "Clearing the provider while Active is required");

        payment.searchProvider("Paystack");
        Assert.assertTrue(payment.getProviderOptions().contains("Paystack"), "Searching 'Paystack' should return Paystack");

        payment.searchProvider("zzz");
        payment.waitForNoProviderOptions();
        Assert.assertTrue(payment.hasNoProviderOptions(), "An unmatched search shows the no-options message");
        Assert.assertEquals(payment.getNoProviderOptionsText(), "No options");

        payment.blurPayment();
        Assert.assertEquals(payment.getProvider(), "", "Unmatched text is cleared on blur");
        Assert.assertTrue(payment.isProviderErrorDisplayed(), "The required error remains while the provider is empty");
    }

    // ------------------------------------------------------------------ Active / Inactive

    @Test(groups = {"regression", "payment"}, description = "PAY-010 Active to Inactive keeps values and drops validation")
    @Story("Conditional validation")
    @Severity(SeverityLevel.CRITICAL)
    public void pay010_activeInactiveSwitching() {
        openPaymentSection();
        payment.selectProvider(CreateBusinessUnitPaymentSection.PAYSTACK);
        payment.enterSecretKey(FAKE_SECRET);

        payment.selectConfigStatus("Active");
        assertRequiredMarked(true);
        Assert.assertEquals(payment.getVisibleErrors(), List.of(), "Switching to Active shows no immediate errors");

        payment.clearProviderWithKeyboard();
        Assert.assertEquals(payment.getProviderError(), PROVIDER_REQUIRED, "Precondition: an error is showing while Active");

        payment.selectConfigStatus("Inactive");
        assertRequiredMarked(false);
        Assert.assertEquals(payment.getVisibleErrors(), List.of(), "Errors disappear when the status becomes Inactive");
        Assert.assertFalse(payment.isProviderMarkedInvalid(), "The provider is no longer invalid");
        Assert.assertEquals(payment.getProvider(), "", "The emptied provider stays empty");
        Assert.assertFalse(payment.isSecretKeyEmpty(), "The secret value is retained");

        payment.clearSecretKey();
        Assert.assertFalse(payment.isSecretKeyErrorDisplayed(), "Clearing while Inactive shows no required error");
        Assert.assertFalse(payment.isSecretKeyMarkedInvalid());
        Assert.assertFalse(payment.isSecretKeyRequiredMarked(), "No required marker while Inactive");

        payment.selectConfigStatus("Active");
        assertRequiredMarked(true);
        Assert.assertFalse(payment.isProviderErrorDisplayed(), "No immediate provider error after switching back to Active");
        Assert.assertFalse(payment.isSecretKeyErrorDisplayed(), "No immediate secret error after switching back to Active");
        Assert.assertEquals(payment.getVisibleErrors(), List.of());
    }

    // ------------------------------------------------------------------ keyboard

    @Test(groups = {"regression", "payment"}, description = "PAY-011 Keyboard behavior of the status and provider lists")
    @Story("Keyboard")
    @Severity(SeverityLevel.NORMAL)
    public void pay011_keyboardBehavior() {
        openPaymentSection();

        payment.openConfigStatusListWithKeyboard();
        Assert.assertTrue(payment.isConfigStatusListOpen(), "ArrowDown should open the status list");
        payment.closeConfigStatusListWithEscape();
        Assert.assertFalse(payment.isConfigStatusListOpen(), "Escape should close the status list");
        Assert.assertEquals(payment.getConfigStatus(), "Inactive", "The selected status is unchanged");
        Assert.assertTrue(dialog.isDialogDisplayed(), "The dialog should remain open");

        payment.openProviderListWithKeyboard();
        Assert.assertTrue(payment.isProviderListOpen(), "ArrowDown should open the provider list");
        payment.selectHighlightedProviderWithKeyboard();
        Assert.assertEquals(payment.getProvider(), "Paystack", "ArrowDown then Enter should select the option");

        payment.openProviderList();
        Assert.assertTrue(payment.isProviderListOpen());
        payment.closeProviderListWithEscape();
        Assert.assertFalse(payment.isProviderListOpen(), "Escape should close the provider list");
        Assert.assertEquals(payment.getProvider(), "Paystack", "Escape keeps the selected provider");
        Assert.assertTrue(dialog.isDialogDisplayed(), "Escape in the provider list must not close the dialog");
    }

    // ------------------------------------------------------------------ secret visibility

    @Test(groups = {"regression", "payment"}, description = "PAY-012 Secret visibility toggle")
    @Story("PAYSTACK Secret Key")
    @Severity(SeverityLevel.NORMAL)
    @Description("Only the input type and whether it is empty are checked; the secret text is never read.")
    public void pay012_secretVisibilityToggle() {
        openPaymentSection();

        payment.enterSecretKey(FAKE_SECRET);
        Assert.assertEquals(payment.getSecretKeyInputType(), "password", "Masked by default");

        payment.toggleSecretKeyVisibility();
        Assert.assertEquals(payment.getSecretKeyInputType(), "text", "The eye reveals the field");
        Assert.assertFalse(payment.isSecretKeyEmpty(), "The value is kept while revealed");

        payment.toggleSecretKeyVisibility();
        Assert.assertEquals(payment.getSecretKeyInputType(), "password", "The eye masks the field again");
        Assert.assertFalse(payment.isSecretKeyEmpty(), "The value is kept after masking again");
    }

    // ------------------------------------------------------------------ navigation and closing

    @Test(groups = {"regression", "payment"}, description = "PAY-013 Payment state persists across section switches")
    @Story("Section navigation")
    @Severity(SeverityLevel.CRITICAL)
    public void pay013_stateSurvivesSectionSwitches() {
        openPaymentSection();
        payment.selectConfigStatus("Active");
        payment.selectProvider(CreateBusinessUnitPaymentSection.PAYSTACK);
        payment.enterSecretKey(FAKE_SECRET);
        payment.toggleSecretKeyVisibility();

        for (String other : List.of(CreateBusinessUnitDialog.BUSINESS_UNIT_DETAILS, "Currency", "ERP Integration")) {
            dialog.selectSection(other);
            Assert.assertEquals(dialog.getSelectedSection(), other, "Selected row after leaving Payment for " + other);
            Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard on " + other);

            dialog.selectSection(CreateBusinessUnitPaymentSection.SECTION);
            Assert.assertEquals(dialog.getSelectedSection(), "Payment Integration", "Selected row after returning from " + other);
            Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard on return");
            Assert.assertEquals(payment.getConfigStatus(), "Active", "Status after visiting " + other);
            Assert.assertEquals(payment.getProvider(), "Paystack", "Provider after visiting " + other);
            Assert.assertFalse(payment.isSecretKeyEmpty(), "Secret should still be filled after visiting " + other);
            Assert.assertEquals(payment.getSecretKeyInputType(), "text", "Secret visibility after visiting " + other);
        }
    }

    @Test(groups = {"regression", "payment"}, description = "PAY-014 Reopening the dialog resets Payment")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void pay014_reopeningResetsPayment() {
        openPaymentSection();
        payment.selectConfigStatus("Active");
        payment.selectProvider(CreateBusinessUnitPaymentSection.PAYSTACK);
        payment.enterSecretKey(FAKE_SECRET);
        payment.toggleSecretKeyVisibility();

        dialog.cancel();
        dashboard.clickAddBusinessUnit();
        CreateBusinessUnitDialog reopened = new CreateBusinessUnitDialog();
        reopened.waitForDialog();
        reopened.selectSection(CreateBusinessUnitPaymentSection.SECTION);

        assertDefaultState();
        reopened.cancel();
    }

    @Test(groups = {"regression", "payment"}, description = "PAY-015 Cancelling with Payment values creates nothing")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void pay015_cancellingCreatesNothing() {
        dashboard = openDashboard();
        List<String> unitsBefore = dashboard.getBusinessUnitNames();
        dashboard.clickAddBusinessUnit();
        dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        dialog.selectSection(CreateBusinessUnitPaymentSection.SECTION);
        payment = new CreateBusinessUnitPaymentSection();
        payment.selectConfigStatus("Active");
        payment.selectProvider(CreateBusinessUnitPaymentSection.PAYSTACK);
        payment.enterSecretKey(FAKE_SECRET);

        dialog.cancel();

        Assert.assertFalse(dialog.isDialogDisplayed(), "The dialog should be closed");
        Assert.assertEquals(dashboard.getBusinessUnitNames(), unitsBefore, "No Business Unit may have been created");
    }
}
