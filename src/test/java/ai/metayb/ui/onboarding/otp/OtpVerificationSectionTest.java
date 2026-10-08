package ai.metayb.ui.onboarding.otp;

import ai.metayb.ui.components.onboarding.OtpFlow;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitDialog;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitOtpSection;
import ai.metayb.ui.pages.onboarding.DashboardPage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.OnboardingAuthenticatedTest;

import java.util.List;

/**
 * OTP Verification section of the Create Business Unit dialog, covering only behavior verified on the live portal.
 * The section is configuration only: four on/off switches saying which flows will require an OTP. It has no OTP
 * delivery or verification, no fields and no validation, so none of that is tested. Every test opens the dialog itself
 * from a fresh dashboard and finishes by leaving it. Nothing is created: Create is never clicked. The "Unsaved
 * changes" chip and the nav-row dot are deliberately not automated.
 *
 * Switch inputs are invisible (opacity 0) and unnamed, so they are never waited on for visibility; each is found
 * through the exact title of its row.
 */
@Epic("Onboarding Portal")
@Feature("Create Business Unit - OTP Verification")
public class OtpVerificationSectionTest extends OnboardingAuthenticatedTest {

    private DashboardPage dashboard;
    private CreateBusinessUnitDialog dialog;
    private CreateBusinessUnitOtpSection otp;

    /** Opens the dialog from a fresh dashboard and shows the OTP Verification section. */
    private void openOtpSection() {
        dashboard = openDashboard();
        dashboard.clickAddBusinessUnit();
        dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        dialog.selectSection(CreateBusinessUnitOtpSection.SECTION);
        otp = new CreateBusinessUnitOtpSection();
    }

    private void assertAllFlows(boolean expectedOn, String context) {
        for (OtpFlow flow : OtpFlow.values()) {
            Assert.assertEquals(otp.isFlowOn(flow), expectedOn, flow.title() + " switch " + context);
        }
    }

    // ------------------------------------------------------------------ section and defaults

    @Test(groups = {"regression", "otp"}, description = "OTP-001 OTP section opens correctly")
    @Story("Section")
    @Severity(SeverityLevel.CRITICAL)
    public void otp001_sectionOpensCorrectly() {
        openOtpSection();

        Assert.assertEquals(dialog.getSelectedSection(), "OTP Verification", "Selected navigation row");
        Assert.assertEquals(otp.getOtpHeading(), "OTP Verification");
        Assert.assertEquals(otp.getOtpDescription(), "Choose which flows require OTP verification before they can be completed.");
        Assert.assertTrue(otp.isOtpSectionDisplayed(), "The OTP panel should be displayed");
        Assert.assertTrue(dialog.isSectionDisplayed("OTP Verification"), "OTP Verification should be the visible panel");
        Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard");
    }

    @Test(groups = {"regression", "otp"}, description = "OTP-002 The four flows with their exact titles and descriptions")
    @Story("Flows")
    @Severity(SeverityLevel.NORMAL)
    public void otp002_fourFlowsWithTitlesAndDescriptions() {
        openOtpSection();

        Assert.assertEquals(otp.getSwitchCount(), 4, "The section has exactly four switches");
        for (OtpFlow flow : OtpFlow.values()) {
            Assert.assertTrue(otp.isFlowDisplayed(flow), flow.title() + " should be listed");
            Assert.assertEquals(otp.getFlowDescription(flow), flow.description(), "Description of " + flow.title());
        }
    }

    @Test(groups = {"regression", "otp"}, description = "OTP-003 Default state")
    @Story("Initial state")
    @Severity(SeverityLevel.NORMAL)
    public void otp003_defaultState() {
        openOtpSection();

        assertAllFlows(false, "should be off by default");
        for (OtpFlow flow : OtpFlow.values()) {
            Assert.assertTrue(otp.isFlowEnabled(flow), flow.title() + " switch should be enabled");
        }
        Assert.assertEquals(otp.getVisibleErrors(), List.of(), "No validation errors");
    }

    // ------------------------------------------------------------------ switches

    @Test(groups = {"regression", "otp"}, description = "OTP-004 Each switch turns on and off independently")
    @Story("Switches")
    @Severity(SeverityLevel.CRITICAL)
    public void otp004_eachSwitchIsIndependent() {
        openOtpSection();

        for (OtpFlow flow : OtpFlow.values()) {
            Assert.assertFalse(otp.isFlowOn(flow), flow.title() + " should start off");

            otp.turnOn(flow);
            Assert.assertTrue(otp.isFlowOn(flow), flow.title() + " should be on after turning it on");
            for (OtpFlow other : OtpFlow.values()) {
                if (other != flow) {
                    Assert.assertFalse(otp.isFlowOn(other), other.title() + " must not change when " + flow.title() + " is turned on");
                }
            }

            otp.turnOff(flow);
            Assert.assertFalse(otp.isFlowOn(flow), flow.title() + " should be off after turning it off");
        }
        Assert.assertEquals(otp.getVisibleErrors(), List.of(), "Toggling never produces errors");
    }

    @Test(groups = {"regression", "otp"}, description = "OTP-005 Clicking a flow title does not toggle its switch")
    @Story("Switches")
    @Severity(SeverityLevel.NORMAL)
    @Description("Observed behavior: only the switch toggles; the title and description text are not wired to it.")
    public void otp005_clickingTitleDoesNotToggle() {
        openOtpSection();

        otp.clickFlowTitle(OtpFlow.PICKUP);

        Assert.assertFalse(otp.isFlowOn(OtpFlow.PICKUP), "Clicking the title text must not switch Pickup on");
        assertAllFlows(false, "should still be off");
    }

    @Test(groups = {"regression", "otp"}, description = "OTP-006 Keyboard behavior of the switches")
    @Story("Keyboard")
    @Severity(SeverityLevel.NORMAL)
    public void otp006_keyboardBehavior() {
        openOtpSection();

        otp.focusFlow(OtpFlow.PICKUP);
        Assert.assertTrue(otp.isFocused(OtpFlow.PICKUP), "Pickup should have keyboard focus");
        Assert.assertFalse(otp.isFlowOn(OtpFlow.PICKUP), "Focusing must not change the switch");

        otp.toggleWithKeyboard(OtpFlow.PICKUP);
        Assert.assertTrue(otp.isFlowOn(OtpFlow.PICKUP), "Space should switch Pickup on");
        otp.toggleWithKeyboard(OtpFlow.PICKUP);
        Assert.assertFalse(otp.isFlowOn(OtpFlow.PICKUP), "Space again should switch Pickup off");

        otp.pressTab(OtpFlow.PICKUP);
        Assert.assertTrue(otp.isFocused(OtpFlow.DELIVERY), "Tab should move focus to the next switch (Delivery)");

        otp.pressEnter(OtpFlow.DELIVERY);
        Assert.assertFalse(otp.isFlowOn(OtpFlow.DELIVERY), "Enter must not toggle the switch");
        assertAllFlows(false, "should all be off after the keyboard checks");
    }

    // ------------------------------------------------------------------ navigation and closing

    @Test(groups = {"regression", "otp"}, description = "OTP-007 OTP state persists across section switches")
    @Story("Section navigation")
    @Severity(SeverityLevel.CRITICAL)
    public void otp007_stateSurvivesSectionSwitches() {
        openOtpSection();
        for (OtpFlow flow : OtpFlow.values()) {
            otp.turnOn(flow);
        }

        for (String other : List.of(CreateBusinessUnitDialog.BUSINESS_UNIT_DETAILS, "Currency", "ERP Integration",
                "Payment Integration", "CDP Integration")) {
            dialog.selectSection(other);
            Assert.assertEquals(dialog.getSelectedSection(), other, "Selected row after leaving OTP for " + other);
            Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard on " + other);

            dialog.selectSection(CreateBusinessUnitOtpSection.SECTION);
            Assert.assertEquals(dialog.getSelectedSection(), "OTP Verification", "Selected row after returning from " + other);
            Assert.assertTrue(dialog.isSectionDisplayed("OTP Verification"), "The OTP panel is visible again after " + other);
            Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard on return");
            assertAllFlows(true, "should still be on after visiting " + other);
        }
    }

    @Test(groups = {"regression", "otp"}, description = "OTP-008 Reopening the dialog resets OTP")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void otp008_reopeningResetsOtp() {
        dashboard = openDashboard();
        List<String> unitsBefore = dashboard.getBusinessUnitNames();
        dashboard.clickAddBusinessUnit();
        dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        dialog.selectSection(CreateBusinessUnitOtpSection.SECTION);
        otp = new CreateBusinessUnitOtpSection();
        for (OtpFlow flow : OtpFlow.values()) {
            otp.turnOn(flow);
        }

        dialog.cancel();
        Assert.assertFalse(dialog.isDialogDisplayed(), "Cancel closes the dialog");
        dashboard.clickAddBusinessUnit();
        CreateBusinessUnitDialog reopened = new CreateBusinessUnitDialog();
        reopened.waitForDialog();
        reopened.selectSection(CreateBusinessUnitOtpSection.SECTION);

        assertAllFlows(false, "should be off again after reopening");
        Assert.assertEquals(otp.getVisibleErrors(), List.of(), "No errors carry over");
        reopened.cancel();
        Assert.assertEquals(dashboard.getBusinessUnitNames(), unitsBefore, "No Business Unit may have been created");
    }

    @Test(groups = {"regression", "otp"}, description = "OTP-009 Cancelling with OTP changes creates nothing")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void otp009_cancellingCreatesNothing() {
        dashboard = openDashboard();
        List<String> unitsBefore = dashboard.getBusinessUnitNames();
        dashboard.clickAddBusinessUnit();
        dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        dialog.selectSection(CreateBusinessUnitOtpSection.SECTION);
        otp = new CreateBusinessUnitOtpSection();
        otp.turnOn(OtpFlow.PICKUP);
        otp.turnOn(OtpFlow.MANUAL_SALE);

        dialog.cancel();

        Assert.assertFalse(dialog.isDialogDisplayed(), "The dialog should be closed");
        Assert.assertEquals(dashboard.getBusinessUnitNames(), unitsBefore, "No Business Unit may have been created");
    }
}
