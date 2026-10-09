package ai.metayb.ui.onboarding.criticale2e;

import ai.metayb.ui.components.onboarding.ErpField;
import ai.metayb.ui.components.onboarding.OtpFlow;
import ai.metayb.ui.pages.onboarding.DashboardPage;
import ai.metayb.ui.pages.onboarding.EditBusinessUnitDialog;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import testUtils.OnboardingAuthenticatedTest;

/**
 * Critical E2E, phase 1: strictly READ-ONLY checks of the existing automation Business Unit "QA Test BU 01".
 *
 * The tests open Edit Details, read what is shown and leave through Cancel. They cannot change anything: the page object
 * they use has no way to type, select, toggle, upload or click Update (or Create), no test opens another Business Unit's
 * menu, and no cron or logout control is touched. Hence no cleanup hook is needed.
 *
 * A baseline that differs from the expected one fails the test with the field name and the observed machine value only;
 * nothing is ever changed to make the test pass. Possibly secret fields are checked for empty/filled only and their
 * values are never read into a message. The "Unsaved changes" chip is deliberately not used: the portal can show it
 * before any edit. Each test signs in and finds the card by its exact name, so the tests are order-independent.
 */
@Epic("Onboarding Portal")
@Feature("Critical E2E - existing Business Unit (read-only)")
public class CriticalE2eReadOnlyTest extends OnboardingAuthenticatedTest {

    private static final String BU_NAME = "QA Test BU 01";
    private static final String EDIT_TITLE = "Edit Business Unit Settings";
    private static final int EXPECTED_ACTIVE = 2;
    private static final int EXPECTED_INACTIVE = 0;

    @Test(groups = {"critical-e2e"}, description = "E2E-001: QA Test BU 01 opens in Edit Details and matches the expected baseline")
    @Story("Baseline configuration")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Opens Edit Details for the exact card 'QA Test BU 01', checks every section against the known baseline "
            + "(secrets only for empty/filled), then cancels. Nothing is changed or saved.")
    public void e2e001VerifyBaselineConfiguration() {
        DashboardPage dashboard = openDashboard();
        Assert.assertTrue(dashboard.getBusinessUnitNames().contains(BU_NAME), "Dashboard should list the Business Unit '" + BU_NAME + "'");

        EditBusinessUnitDialog edit = new EditBusinessUnitDialog();
        edit.open(BU_NAME);

        Assert.assertTrue(edit.isOpen(), "The Edit Details dialog should be open");
        Assert.assertTrue(edit.isOnDashboardRoute(), "Edit Details should open over the dashboard without changing the route");
        Assert.assertEquals(edit.getTitle(), EDIT_TITLE, "Dialog title");
        for (String section : EditBusinessUnitDialog.SECTIONS) {
            Assert.assertTrue(edit.isSectionDisplayed(section), "Section '" + section + "' should be present");
        }

        // Every difference is collected, so one run shows the whole drift without any value being exposed.
        SoftAssert baseline = new SoftAssert();
        baseline.assertEquals(edit.getName(), BU_NAME, "Business Unit name");
        baseline.assertEquals(edit.getBusinessUnitStatus(), "active", "Business Unit status");
        baseline.assertTrue(edit.getCurrency().startsWith("INR"), "Currency should be INR");

        baseline.assertEquals(edit.getErpStatus(), "inactive", "ERP status");
        baseline.assertTrue(edit.isErpProviderEmpty(), "ERP provider should be empty");
        for (ErpField field : ErpField.values()) {
            baseline.assertTrue(edit.isErpFieldEmpty(field), "ERP field '" + field.label() + "' should be empty");
        }

        baseline.assertEquals(edit.getPaymentStatus(), "inactive", "Payment status");
        baseline.assertTrue(edit.isPaymentProviderEmpty(), "Payment provider should be empty");
        baseline.assertTrue(edit.isPaymentSecretEmpty(), "Payment secret should be empty");

        baseline.assertEquals(edit.getCdpStatus(), "inactive", "CDP status");
        baseline.assertTrue(edit.isCdpCompanyNameEmpty(), "CDP company name should be empty");
        baseline.assertTrue(edit.isCdpCountryEmpty(), "CDP country should be empty");

        for (OtpFlow flow : OtpFlow.values()) {
            baseline.assertFalse(edit.isOtpOn(flow), "OTP '" + flow.title() + "' should be off");
        }

        baseline.assertEquals(edit.getGeofenceStatus(), "inactive", "Geofence status");
        baseline.assertTrue(edit.isGeofenceRadiusEmpty(), "Geofence radius should be empty");

        baseline.assertEquals(edit.getActivityStatus(), "inactive", "Activity Communication status");
        baseline.assertTrue(edit.isActivityChannelEmpty(), "Activity Communication channel should be empty");
        baseline.assertFalse(edit.isActivityApiKeyFieldPresent(), "No Activity API key field should be present without a channel");
        baseline.assertFalse(edit.isActivitySenderIdFieldPresent(), "No Activity Sender ID field should be present without a channel");
        baseline.assertAll();

        edit.cancel();
        Assert.assertFalse(edit.isOpen(), "Cancel should close the dialog");
        Assert.assertTrue(dashboard.isDashboardDisplayed(), "The dashboard should remain available after Cancel");
    }

    @Test(groups = {"critical-e2e"}, description = "E2E-004: the Dashboard card and tenant counts are as expected and unchanged by opening and cancelling Edit Details")
    @Story("Dashboard state")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Checks the exact 'QA Test BU 01' card (Active, 'Created on' text) and the 2 Active / 0 Inactive counts, "
            + "opens and cancels Edit Details, and verifies the card and counts are unchanged.")
    public void e2e004VerifyDashboardCardAndCounts() {
        DashboardPage dashboard = openDashboard();

        Assert.assertTrue(dashboard.getBusinessUnitNames().contains(BU_NAME), "Dashboard should list the Business Unit '" + BU_NAME + "'");
        String statusBefore = dashboard.getBusinessUnitStatus(BU_NAME);
        String createdBefore = dashboard.getBusinessUnitCreatedText(BU_NAME);
        int activeBefore = dashboard.getActiveBusinessUnitCount();
        int inactiveBefore = dashboard.getInactiveBusinessUnitCount();

        Assert.assertEquals(statusBefore, "Active", "Card status");
        Assert.assertTrue(createdBefore.startsWith("Created on") && !createdBefore.replace("Created on", "").isBlank(),
                "The card should show a non-blank 'Created on' date");
        Assert.assertEquals(activeBefore, EXPECTED_ACTIVE, "Active Business Unit count (expected tenant baseline)");
        Assert.assertEquals(inactiveBefore, EXPECTED_INACTIVE, "Inactive Business Unit count (expected tenant baseline)");

        EditBusinessUnitDialog edit = new EditBusinessUnitDialog();
        edit.open(BU_NAME);
        Assert.assertEquals(edit.getName(), BU_NAME, "Edit Details should be for the same Business Unit");
        edit.cancel();

        Assert.assertTrue(dashboard.isDashboardDisplayed(), "The dashboard should remain available after Cancel");
        Assert.assertEquals(dashboard.getBusinessUnitStatus(BU_NAME), statusBefore, "Card status after open and cancel");
        Assert.assertEquals(dashboard.getBusinessUnitCreatedText(BU_NAME), createdBefore, "Created text after open and cancel");
        Assert.assertEquals(dashboard.getActiveBusinessUnitCount(), activeBefore, "Active count after open and cancel");
        Assert.assertEquals(dashboard.getInactiveBusinessUnitCount(), inactiveBefore, "Inactive count after open and cancel");
    }
}
