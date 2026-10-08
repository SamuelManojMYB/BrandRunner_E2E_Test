package ai.metayb.ui.onboarding.createbusinessunit;

import ai.metayb.ui.components.onboarding.ErpField;
import ai.metayb.ui.components.onboarding.OtpFlow;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitActivitySection;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitCdpSection;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitCurrencySection;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitDialog;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitDialogFrame;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitErpSection;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitGeofenceSection;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitOtpSection;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitPaymentSection;
import ai.metayb.ui.pages.onboarding.DashboardPage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.OnboardingAuthenticatedTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dialog-level behavior of Create Business Unit, and one persistence check across all eight sections. Strictly local:
 * nothing here, and nothing in the page objects it uses, can click Create, upload a logo or send anything, so no
 * Business Unit can be created or changed. Every test leaves the dialog by Cancel, X or the back arrow, and every value
 * entered is harmless placeholder text that never leaves the browser. The only secret-like value, a fake API key, is
 * typed with typeSecret and never read, asserted or logged.
 *
 * What is asserted about closing is what the portal does today, not a claim that it is desirable: Escape and a click
 * outside do not close the dialog, and closing asks for no confirmation and discards unsaved values.
 *
 * The back arrow and the X share the aria-label "Cancel & close", so the existing page object tells them apart by
 * document order (the arrow is the first, the X the last); both only close the dialog.
 */
@Epic("Onboarding Portal")
@Feature("Create Business Unit - Dialog")
public class CreateBusinessUnitDialogTest extends OnboardingAuthenticatedTest {

    private static final String TEMP_NAME = "Temp unsaved BU";
    private static final String FAKE_API_KEY = "dialog-key-not-real-0000";
    private static final String SENDER_ID = "sender-test-01";
    private static final String SQL_ENDPOINT = "fake-sql-endpoint";
    private static final String COMPANY = "Temp CDP Co";
    private static final int STAYS_OPEN_SECONDS = 3;

    private DashboardPage dashboard;
    private CreateBusinessUnitDialog dialog;
    private CreateBusinessUnitDialogFrame frame;

    // The section page objects only ever read and edit unsaved text; none can click Create.
    private CreateBusinessUnitCurrencySection currency;
    private CreateBusinessUnitErpSection erp;
    private CreateBusinessUnitPaymentSection payment;
    private CreateBusinessUnitCdpSection cdp;
    private CreateBusinessUnitOtpSection otp;
    private CreateBusinessUnitGeofenceSection geofence;
    private CreateBusinessUnitActivitySection activity;

    private void openCreateDialog() {
        dashboard = openDashboard();
        dashboard.clickAddBusinessUnit();
        useOpenDialog();
    }

    /** Binds the page objects to the dialog that is currently open. */
    private void useOpenDialog() {
        dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        frame = new CreateBusinessUnitDialogFrame();
        currency = new CreateBusinessUnitCurrencySection();
        erp = new CreateBusinessUnitErpSection();
        payment = new CreateBusinessUnitPaymentSection();
        cdp = new CreateBusinessUnitCdpSection();
        otp = new CreateBusinessUnitOtpSection();
        geofence = new CreateBusinessUnitGeofenceSection();
        activity = new CreateBusinessUnitActivitySection();
    }

    private void makeHarmlessChange() {
        dialog.enterBusinessUnitName(TEMP_NAME);
    }

    private void assertClosedCleanly(List<String> unitsBefore) {
        Assert.assertFalse(frame.isOpen(), "The dialog should be closed");
        Assert.assertFalse(frame.isConfirmationDisplayed(), "Closing must not ask for a confirmation");
        Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must remain /dashboard");
        Assert.assertTrue(dashboard.isDashboardDisplayed(), "The signed-in dashboard should be displayed");
        Assert.assertEquals(dashboard.getBusinessUnitNames(), unitsBefore, "No Business Unit may have been created");
    }

    // ------------------------------------------------------------------ dialog frame

    @Test(groups = {"regression", "createbu"}, description = "CBU-001 The Create Business Unit dialog opens as a modal dialog")
    @Story("Dialog")
    @Severity(SeverityLevel.CRITICAL)
    public void cbu001_dialogOpens() {
        openCreateDialog();

        Assert.assertTrue(frame.isOpen(), "The dialog should be displayed");
        Assert.assertEquals(frame.getRole(), "dialog", "Dialog role");
        Assert.assertTrue(frame.isModal(), "The dialog should be modal (aria-modal=true)");
        Assert.assertEquals(frame.getTitleText(), "Create Business Unit", "Dialog title");
        Assert.assertTrue(dialog.getTitle().startsWith("Create Business Unit"), "Title was: " + dialog.getTitle());
        Assert.assertTrue(frame.isNewChipDisplayed(), "The 'New' chip should be shown");
        Assert.assertFalse(frame.isUnsavedChangesChipDisplayed(), "No 'Unsaved changes' chip on a fresh dialog");
        Assert.assertTrue(dialog.isBusinessUnitDetailsSelected(), "A fresh dialog opens on Business Unit Details");
        Assert.assertTrue(dialog.isOnDashboardRoute(), "The dialog opens over /dashboard");

        dialog.cancel();
    }

    @Test(groups = {"regression", "createbu"}, description = "CBU-002 The Unsaved changes chip follows the values")
    @Story("Dirty state")
    @Severity(SeverityLevel.NORMAL)
    @Description("Uses an OTP switch (no validation involved): changing it shows the chip, restoring the default removes it.")
    public void cbu002_unsavedChangesChip() {
        openCreateDialog();
        dialog.selectSection(CreateBusinessUnitOtpSection.SECTION);
        Assert.assertFalse(frame.isUnsavedChangesChipDisplayed(), "Precondition: no chip before any change");

        otp.turnOn(OtpFlow.PICKUP);
        frame.waitForUnsavedChangesChip();
        Assert.assertTrue(frame.isUnsavedChangesChipDisplayed(), "The chip should appear once a value differs from its default");

        otp.turnOff(OtpFlow.PICKUP);
        frame.waitForUnsavedChangesChipToDisappear();
        Assert.assertFalse(frame.isUnsavedChangesChipDisplayed(), "The chip should disappear when the value is back at its default");

        dialog.cancel();
    }

    // ------------------------------------------------------------------ dismiss attempts (observed behavior)

    @Test(groups = {"regression", "createbu"}, description = "CBU-003 Escape does not close the dialog (observed behavior)")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    @Description("Documents what the portal does today, not what it should do.")
    public void cbu003_escapeDoesNotClose() {
        openCreateDialog();
        makeHarmlessChange();

        frame.pressEscape();

        Assert.assertTrue(frame.staysOpenFor(STAYS_OPEN_SECONDS), "Escape should not close the dialog");
        Assert.assertTrue(frame.isOpen(), "The dialog should still be displayed");
        Assert.assertEquals(dialog.getBusinessUnitName(), TEMP_NAME, "The unsaved value is untouched");
        Assert.assertFalse(frame.isConfirmationDisplayed(), "No confirmation appears");

        dialog.cancel();
    }

    @Test(groups = {"regression", "createbu"}, description = "CBU-004 A click outside does not close the dialog (observed behavior)")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    @Description("Documents what the portal does today. The click lands on the dialog's container, which handles outside clicks.")
    public void cbu004_outsideClickDoesNotClose() {
        openCreateDialog();
        makeHarmlessChange();

        frame.clickOutsideDialog();

        Assert.assertTrue(frame.staysOpenFor(STAYS_OPEN_SECONDS), "A click outside should not close the dialog");
        Assert.assertTrue(frame.isOpen(), "The dialog should still be displayed");
        Assert.assertEquals(dialog.getBusinessUnitName(), TEMP_NAME, "The unsaved value is untouched");
        Assert.assertFalse(frame.isConfirmationDisplayed(), "No confirmation appears");

        dialog.cancel();
    }

    // ------------------------------------------------------------------ closing

    @Test(groups = {"regression", "createbu"}, description = "CBU-005 Cancel closes the dialog at once with no confirmation")
    @Story("Closing")
    @Severity(SeverityLevel.CRITICAL)
    public void cbu005_cancelClosesWithoutConfirmation() {
        openCreateDialog();
        List<String> unitsBefore = dashboard.getBusinessUnitNames();
        makeHarmlessChange();
        frame.waitForUnsavedChangesChip();
        Assert.assertTrue(frame.isUnsavedChangesChipDisplayed(), "Precondition: there are unsaved changes");

        dialog.cancel();

        assertClosedCleanly(unitsBefore);
    }

    @Test(groups = {"regression", "createbu"}, description = "CBU-006 The X closes the dialog with no confirmation")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void cbu006_xClosesWithoutConfirmation() {
        openCreateDialog();
        List<String> unitsBefore = dashboard.getBusinessUnitNames();
        makeHarmlessChange();

        dialog.closeWithX();

        assertClosedCleanly(unitsBefore);
    }

    @Test(groups = {"regression", "createbu"}, description = "CBU-007 The back arrow closes the dialog with no confirmation")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void cbu007_backArrowClosesWithoutConfirmation() {
        openCreateDialog();
        List<String> unitsBefore = dashboard.getBusinessUnitNames();
        makeHarmlessChange();

        dialog.closeWithBackArrow();

        assertClosedCleanly(unitsBefore);
    }

    @Test(groups = {"regression", "createbu"}, description = "CBU-008 A reopened dialog starts fresh")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void cbu008_reopenStartsFresh() {
        openCreateDialog();
        List<String> unitsBefore = dashboard.getBusinessUnitNames();
        makeHarmlessChange();
        dialog.selectSection(CreateBusinessUnitCurrencySection.SECTION);
        currency.selectCurrency("INR");
        dialog.cancel();

        dashboard.clickAddBusinessUnit();
        useOpenDialog();

        Assert.assertTrue(dialog.isBusinessUnitDetailsSelected(), "A reopened dialog starts on Business Unit Details");
        Assert.assertFalse(frame.isUnsavedChangesChipDisplayed(), "No 'Unsaved changes' chip after reopening");
        Assert.assertEquals(dialog.getBusinessUnitName(), "", "The previous temporary name is gone");
        dialog.selectSection(CreateBusinessUnitCurrencySection.SECTION);
        Assert.assertEquals(currency.getSelectedCurrency(), "", "The previous currency is gone");

        dialog.cancel();
        Assert.assertEquals(dashboard.getBusinessUnitNames(), unitsBefore, "No Business Unit may have been created");
    }

    // ------------------------------------------------------------------ all eight sections

    @Test(groups = {"regression", "createbu"}, description = "CBU-009 Values in all eight sections persist while the dialog stays open")
    @Story("Section navigation")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Harmless values in every section, checked on a pass through the sections in order and again in reverse order.")
    public void cbu009_valuesPersistAcrossAllSections() {
        openCreateDialog();
        List<String> unitsBefore = dashboard.getBusinessUnitNames();
        fillAllSections();

        for (String section : CreateBusinessUnitDialog.SECTIONS) {
            dialog.selectSection(section);
            Assert.assertEquals(dialog.getSelectedSection(), section, "Selected row (forward pass)");
            Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard on " + section);
            verifyFilledSection(section);
        }

        List<String> reversed = new ArrayList<>(CreateBusinessUnitDialog.SECTIONS);
        Collections.reverse(reversed);
        for (String section : reversed) {
            dialog.selectSection(section);
            Assert.assertEquals(dialog.getSelectedSection(), section, "Selected row (reverse pass)");
            verifyFilledSection(section);
        }
        frame.waitForUnsavedChangesChip();
        Assert.assertTrue(frame.isUnsavedChangesChipDisplayed(), "The dialog still reports unsaved changes");

        dialog.cancel();
        assertClosedCleanly(unitsBefore);
    }

    @Test(groups = {"regression", "createbu"}, description = "CBU-010 Reopening resets every section")
    @Story("Closing")
    @Severity(SeverityLevel.CRITICAL)
    public void cbu010_reopenResetsEverySection() {
        openCreateDialog();
        List<String> unitsBefore = dashboard.getBusinessUnitNames();
        fillAllSections();
        dialog.cancel();

        dashboard.clickAddBusinessUnit();
        useOpenDialog();

        Assert.assertTrue(dialog.isBusinessUnitDetailsSelected(), "A reopened dialog starts on Business Unit Details");
        Assert.assertFalse(frame.isUnsavedChangesChipDisplayed(), "No 'Unsaved changes' chip after reopening");
        for (String section : CreateBusinessUnitDialog.SECTIONS) {
            dialog.selectSection(section);
            verifyDefaultSection(section);
        }

        dialog.cancel();
        Assert.assertEquals(dashboard.getBusinessUnitNames(), unitsBefore, "No Business Unit may have been created");
    }

    // ------------------------------------------------------------------ per-section values

    /** Puts a harmless value in every section. Nothing secret except one fake API key, and nothing is submitted. */
    private void fillAllSections() {
        dialog.selectSection(CreateBusinessUnitDialog.BUSINESS_UNIT_DETAILS);
        dialog.enterBusinessUnitName(TEMP_NAME);

        dialog.selectSection(CreateBusinessUnitCurrencySection.SECTION);
        currency.selectCurrency("INR");

        dialog.selectSection(CreateBusinessUnitErpSection.SECTION);
        erp.selectConfigStatus("Active");
        erp.selectProvider("Fabric");
        erp.enterFieldValue(ErpField.SQL_ENDPOINT, SQL_ENDPOINT);

        dialog.selectSection(CreateBusinessUnitPaymentSection.SECTION);
        payment.selectConfigStatus("Active");
        payment.selectProvider(CreateBusinessUnitPaymentSection.PAYSTACK);

        dialog.selectSection(CreateBusinessUnitCdpSection.SECTION);
        cdp.selectConfigStatus("Active");
        cdp.enterCompanyName(COMPANY);
        cdp.selectCountry(CreateBusinessUnitCdpSection.GHANA);

        dialog.selectSection(CreateBusinessUnitOtpSection.SECTION);
        otp.turnOn(OtpFlow.PICKUP);

        dialog.selectSection(CreateBusinessUnitGeofenceSection.SECTION);
        geofence.selectConfigStatus("Active");
        geofence.enterRadius("250");

        dialog.selectSection(CreateBusinessUnitActivitySection.SECTION);
        activity.selectStatus("Active");
        activity.selectChannel(CreateBusinessUnitActivitySection.INFINITY_CONNECT);
        activity.enterSenderId(SENDER_ID);
        activity.enterApiKey(FAKE_API_KEY);
    }

    private void verifyFilledSection(String section) {
        switch (section) {
            case CreateBusinessUnitDialog.BUSINESS_UNIT_DETAILS ->
                    Assert.assertEquals(dialog.getBusinessUnitName(), TEMP_NAME, "Business Unit name");
            case CreateBusinessUnitCurrencySection.SECTION ->
                    Assert.assertTrue(currency.getSelectedCurrency().startsWith("INR"), "Currency was: " + currency.getSelectedCurrency());
            case CreateBusinessUnitErpSection.SECTION -> {
                Assert.assertEquals(erp.getConfigStatus(), "Active", "ERP status");
                Assert.assertEquals(erp.getProvider(), "Fabric", "ERP provider");
                Assert.assertEquals(erp.getFieldValue(ErpField.SQL_ENDPOINT), SQL_ENDPOINT, "ERP SQL Endpoint");
            }
            case CreateBusinessUnitPaymentSection.SECTION -> {
                Assert.assertEquals(payment.getConfigStatus(), "Active", "Payment status");
                Assert.assertEquals(payment.getProvider(), CreateBusinessUnitPaymentSection.PAYSTACK, "Payment provider");
                Assert.assertTrue(payment.isSecretKeyEmpty(), "No payment secret was ever entered");
            }
            case CreateBusinessUnitCdpSection.SECTION -> {
                Assert.assertEquals(cdp.getConfigStatus(), "Active", "CDP status");
                Assert.assertEquals(cdp.getCompanyName(), COMPANY, "CDP company name");
                Assert.assertEquals(cdp.getCountry(), CreateBusinessUnitCdpSection.GHANA, "CDP country");
            }
            case CreateBusinessUnitOtpSection.SECTION -> {
                Assert.assertTrue(otp.isFlowOn(OtpFlow.PICKUP), "Pickup should still be on");
                for (OtpFlow flow : OtpFlow.values()) {
                    if (flow != OtpFlow.PICKUP) {
                        Assert.assertFalse(otp.isFlowOn(flow), flow.title() + " should still be off");
                    }
                }
            }
            case CreateBusinessUnitGeofenceSection.SECTION -> {
                Assert.assertEquals(geofence.getConfigStatus(), "Active", "Geofence status");
                Assert.assertEquals(geofence.getRadius(), "250", "Geofence radius");
            }
            case CreateBusinessUnitActivitySection.SECTION -> {
                Assert.assertEquals(activity.getStatus(), "Active", "Activity status");
                Assert.assertEquals(activity.getChannel(), CreateBusinessUnitActivitySection.INFINITY_CONNECT, "Activity channel");
                Assert.assertEquals(activity.getSenderId(), SENDER_ID, "Sender ID");
                Assert.assertFalse(activity.isApiKeyEmpty(), "The API key should still be filled");
                Assert.assertEquals(activity.getApiKeyInputType(), "password", "The API key should still be masked");
            }
            default -> Assert.fail("Unknown section " + section);
        }
    }

    private void verifyDefaultSection(String section) {
        switch (section) {
            case CreateBusinessUnitDialog.BUSINESS_UNIT_DETAILS ->
                    Assert.assertEquals(dialog.getBusinessUnitName(), "", "Business Unit name should be empty");
            case CreateBusinessUnitCurrencySection.SECTION ->
                    Assert.assertEquals(currency.getSelectedCurrency(), "", "Currency should be empty");
            case CreateBusinessUnitErpSection.SECTION -> {
                Assert.assertEquals(erp.getConfigStatus(), "Inactive", "ERP status should be Inactive");
                Assert.assertEquals(erp.getProvider(), "", "ERP provider should be empty");
                Assert.assertTrue(erp.isFieldEmpty(ErpField.SQL_ENDPOINT), "ERP SQL Endpoint should be empty");
            }
            case CreateBusinessUnitPaymentSection.SECTION -> {
                Assert.assertEquals(payment.getConfigStatus(), "Inactive", "Payment status should be Inactive");
                Assert.assertEquals(payment.getProvider(), "", "Payment provider should be empty");
            }
            case CreateBusinessUnitCdpSection.SECTION -> {
                Assert.assertEquals(cdp.getConfigStatus(), "Inactive", "CDP status should be Inactive");
                Assert.assertTrue(cdp.isCompanyNameEmpty(), "CDP company name should be empty");
                Assert.assertEquals(cdp.getCountry(), "", "CDP country should be empty");
            }
            case CreateBusinessUnitOtpSection.SECTION -> {
                for (OtpFlow flow : OtpFlow.values()) {
                    Assert.assertFalse(otp.isFlowOn(flow), flow.title() + " should be off");
                }
            }
            case CreateBusinessUnitGeofenceSection.SECTION -> {
                Assert.assertEquals(geofence.getConfigStatus(), "Inactive", "Geofence status should be Inactive");
                Assert.assertTrue(geofence.isRadiusEmpty(), "Geofence radius should be empty");
            }
            case CreateBusinessUnitActivitySection.SECTION -> {
                Assert.assertTrue(activity.isAccordionExpanded(), "The accordion should start expanded");
                Assert.assertEquals(activity.getStatus(), "Inactive", "Activity status should be Inactive");
                Assert.assertEquals(activity.getChannel(), "", "Activity channel should be empty");
                Assert.assertFalse(activity.isApiKeyDisplayed(), "API Key should be hidden again");
                Assert.assertFalse(activity.isSenderIdDisplayed(), "Sender ID should be hidden again");
            }
            default -> Assert.fail("Unknown section " + section);
        }
    }
}
