package ai.metayb.ui.onboarding.geofence;

import ai.metayb.ui.pages.onboarding.CreateBusinessUnitDialog;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitGeofenceSection;
import ai.metayb.ui.pages.onboarding.DashboardPage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.OnboardingAuthenticatedTest;

import java.util.List;

/**
 * Geofence section of the Create Business Unit dialog, covering only behavior verified on the live portal. Every test
 * opens the dialog itself from a fresh dashboard and finishes by leaving it. Nothing is created: Create is never
 * clicked, and every value entered only changes unsaved form state.
 *
 * Observed rules the tests rely on, deliberately not "corrected":
 *  - validation is edit-driven (never raised by focus/blur or by changing the status);
 *  - "Radius is required." applies only while the status is Active, but the whole-number and range rules apply in
 *    either status;
 *  - switching Active to Inactive clears the current error and keeps the value, and switching back shows no error until
 *    the field is edited again;
 *  - an error raised while Inactive is still showing after switching to Active.
 * Leading zeros, whitespace-only and pasted values, submit-time validation, the "Unsaved changes" chip and the nav-row
 * dot are deliberately not tested.
 */
@Epic("Onboarding Portal")
@Feature("Create Business Unit - Geofence")
public class GeofenceSectionTest extends OnboardingAuthenticatedTest {

    private static final String HELP_TEXT = "Distance in metres, e.g. 100.";
    private static final String REQUIRED = "Radius is required.";
    private static final String WHOLE_NUMBER = "Radius must be a whole number.";
    private static final String RANGE = "Radius must be between 1 and 100000 metres.";

    private DashboardPage dashboard;
    private CreateBusinessUnitDialog dialog;
    private CreateBusinessUnitGeofenceSection geofence;

    /** Opens the dialog from a fresh dashboard and shows the Geofence section. */
    private void openGeofenceSection() {
        dashboard = openDashboard();
        dashboard.clickAddBusinessUnit();
        dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        dialog.selectSection(CreateBusinessUnitGeofenceSection.SECTION);
        geofence = new CreateBusinessUnitGeofenceSection();
    }

    private void assertDefaultState() {
        Assert.assertEquals(geofence.getConfigStatus(), "Inactive", "Default status");
        Assert.assertEquals(geofence.getConfigStatusValue(), "inactive", "Default status value");
        Assert.assertTrue(geofence.isRadiusEmpty(), "Radius should be empty");
        Assert.assertEquals(geofence.getRadiusUnit(), "m", "Radius unit");
        Assert.assertEquals(geofence.getRadiusHelpText(), HELP_TEXT, "Help text under Radius");
        Assert.assertFalse(geofence.isRadiusRequiredMarked(),
                "Radius should not be required (label shows '" + geofence.getRadiusLabel() + "')");
        Assert.assertFalse(geofence.isRadiusErrorDisplayed(), "No error under Radius");
        Assert.assertEquals(geofence.getVisibleErrors(), List.of(), "No validation errors");
    }

    // ------------------------------------------------------------------ section and defaults

    @Test(groups = {"regression", "geofence"}, description = "GEO-001 Geofence section opens correctly")
    @Story("Section")
    @Severity(SeverityLevel.CRITICAL)
    public void geo001_sectionOpensCorrectly() {
        openGeofenceSection();

        Assert.assertEquals(dialog.getSelectedSection(), "Geofence", "Selected navigation row");
        Assert.assertEquals(geofence.getGeofenceHeading(), "Geofence");
        Assert.assertEquals(geofence.getGeofenceDescription(),
                "Set how close a user must be to a location before they can act on it.");
        Assert.assertTrue(geofence.isGeofenceSectionDisplayed(), "The Geofence panel should be displayed");
        Assert.assertTrue(dialog.isSectionDisplayed("Geofence"), "Geofence should be the visible panel");
        Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard");
    }

    @Test(groups = {"regression", "geofence"}, description = "GEO-002 Default state")
    @Story("Initial state")
    @Severity(SeverityLevel.NORMAL)
    public void geo002_defaultState() {
        openGeofenceSection();

        assertDefaultState();
        Assert.assertEquals(geofence.getRadiusInputType(), "text", "Radius input type");
        Assert.assertEquals(geofence.getRadiusInputMode(), "numeric", "Radius input mode");
        Assert.assertEquals(geofence.getRadiusPattern(), "[0-9]*", "Radius pattern");
        Assert.assertEquals(geofence.getMaxLength(), 6, "Radius maxlength");
        Assert.assertTrue(geofence.isRadiusDescribedByItsMessage(), "aria-describedby should point at the help text");
    }

    @Test(groups = {"regression", "geofence"}, description = "GEO-003 Config Status options")
    @Story("Geofence Config Status")
    @Severity(SeverityLevel.NORMAL)
    public void geo003_configStatusOptions() {
        openGeofenceSection();

        geofence.openConfigStatusList();

        Assert.assertEquals(geofence.getConfigStatusOptions(), List.of("Active", "Inactive"), "Status options");
        Assert.assertTrue(geofence.isConfigStatusOptionSelected("Inactive"), "Inactive should be marked selected");
        Assert.assertFalse(geofence.isConfigStatusOptionSelected("Active"), "Active should not be marked selected");
        geofence.closeConfigStatusListWithEscape();
        Assert.assertEquals(geofence.getConfigStatus(), "Inactive", "Displayed value after viewing the options");
    }

    // ------------------------------------------------------------------ Active branch

    @Test(groups = {"regression", "geofence"}, description = "GEO-004 Selecting Active marks Radius required")
    @Story("Conditional validation")
    @Severity(SeverityLevel.CRITICAL)
    public void geo004_activeMarksRadiusRequired() {
        openGeofenceSection();

        geofence.selectConfigStatus("Active");

        Assert.assertEquals(geofence.getConfigStatusValue(), "active");
        Assert.assertTrue(geofence.isRadiusRequiredMarked(), "Radius should be marked required (label shows '" + geofence.getRadiusLabel() + "')");
        Assert.assertEquals(geofence.getVisibleErrors(), List.of(), "Untouched Radius must not show an error immediately");
        Assert.assertEquals(geofence.getRadiusHelpText(), HELP_TEXT, "The help text stays");
    }

    @Test(groups = {"regression", "geofence"}, description = "GEO-005 Untouched Active field shows no error on blur")
    @Story("Conditional validation")
    @Severity(SeverityLevel.NORMAL)
    public void geo005_untouchedActiveFieldShowsNoErrorOnBlur() {
        openGeofenceSection();
        geofence.selectConfigStatus("Active");

        geofence.focusRadius();
        geofence.blurGeofence();

        Assert.assertFalse(geofence.isRadiusErrorDisplayed(), "Focus and blur alone must not validate");
        Assert.assertFalse(geofence.isRadiusMarkedInvalid());
        Assert.assertEquals(geofence.getVisibleErrors(), List.of());
    }

    @Test(groups = {"regression", "geofence"}, description = "GEO-006 Required validation")
    @Story("Radius")
    @Severity(SeverityLevel.CRITICAL)
    public void geo006_requiredValidation() {
        openGeofenceSection();
        geofence.selectConfigStatus("Active");

        geofence.enterRadius("100");
        geofence.clearRadius();

        Assert.assertEquals(geofence.getRadiusError(), REQUIRED, "Error after editing Radius to empty");
        Assert.assertTrue(geofence.isRadiusMarkedInvalid(), "Radius should be aria-invalid");
        Assert.assertTrue(geofence.isRadiusDescribedByItsMessage(), "aria-describedby should point at the message");

        geofence.enterRadius("100");
        geofence.waitForRadiusErrorToDisappear();
        Assert.assertFalse(geofence.isRadiusMarkedInvalid(), "Radius should be valid once a value is supplied");
        Assert.assertEquals(geofence.getRadiusHelpText(), HELP_TEXT, "The help text returns");
        Assert.assertEquals(geofence.getVisibleErrors(), List.of());
    }

    // ------------------------------------------------------------------ format and range

    @Test(groups = {"regression", "geofence"}, description = "GEO-007 Whole-number validation")
    @Story("Radius")
    @Severity(SeverityLevel.CRITICAL)
    public void geo007_wholeNumberValidation() {
        openGeofenceSection();
        geofence.selectConfigStatus("Active");

        geofence.enterRadius("abc");
        Assert.assertEquals(geofence.getRadiusError(), WHOLE_NUMBER, "Error for text");
        Assert.assertTrue(geofence.isRadiusMarkedInvalid(), "Radius should be aria-invalid for text");
        Assert.assertTrue(geofence.isRadiusDescribedByItsMessage());

        geofence.enterRadius("1.5");
        Assert.assertEquals(geofence.getRadiusError(), WHOLE_NUMBER, "Error for a decimal");
        Assert.assertTrue(geofence.isRadiusMarkedInvalid(), "Radius should be aria-invalid for a decimal");

        geofence.enterRadius("100");
        geofence.waitForRadiusErrorToDisappear();
        Assert.assertFalse(geofence.isRadiusMarkedInvalid(), "A whole number clears the error");
    }

    @Test(groups = {"regression", "geofence"}, description = "GEO-008 Range validation")
    @Story("Radius")
    @Severity(SeverityLevel.CRITICAL)
    public void geo008_rangeValidation() {
        openGeofenceSection();
        geofence.selectConfigStatus("Active");

        geofence.enterRadius("0");
        Assert.assertEquals(geofence.getRadiusError(), RANGE, "Error for 0");
        Assert.assertTrue(geofence.isRadiusMarkedInvalid(), "0 should be aria-invalid");

        geofence.enterRadius("1");
        geofence.waitForRadiusErrorToDisappear();
        Assert.assertFalse(geofence.isRadiusMarkedInvalid(), "1 is the smallest valid radius");
        Assert.assertEquals(geofence.getRadiusHelpText(), HELP_TEXT);

        geofence.enterRadius("100001");
        Assert.assertEquals(geofence.getRadiusError(), RANGE, "Error for 100001");
        Assert.assertTrue(geofence.isRadiusMarkedInvalid(), "100001 should be aria-invalid");

        geofence.enterRadius("100000");
        geofence.waitForRadiusErrorToDisappear();
        Assert.assertFalse(geofence.isRadiusMarkedInvalid(), "100000 is the largest valid radius");
        Assert.assertEquals(geofence.getRadiusHelpText(), HELP_TEXT);
    }

    @Test(groups = {"regression", "geofence"}, description = "GEO-009 Radius accepts at most six characters")
    @Story("Radius")
    @Severity(SeverityLevel.NORMAL)
    public void geo009_maxLength() {
        openGeofenceSection();

        geofence.enterRadius("123456");
        Assert.assertEquals(geofence.getRadius(), "123456", "Six digits are accepted");

        geofence.appendToRadius("7");

        Assert.assertEquals(geofence.getRadius(), "123456", "A seventh digit must not be accepted");
        Assert.assertEquals(geofence.getMaxLength(), 6, "maxlength attribute");
    }

    // ------------------------------------------------------------------ Active / Inactive

    @Test(groups = {"regression", "geofence"}, description = "GEO-010 Active to Inactive clears the error and keeps the value")
    @Story("Conditional validation")
    @Severity(SeverityLevel.CRITICAL)
    public void geo010_activeInactiveSwitching() {
        openGeofenceSection();
        geofence.selectConfigStatus("Active");
        geofence.enterRadius("100001");
        Assert.assertEquals(geofence.getRadiusError(), RANGE, "Precondition: an error is showing while Active");

        geofence.selectConfigStatus("Inactive");
        Assert.assertFalse(geofence.isRadiusRequiredMarked(), "The required marker disappears when Inactive");
        geofence.waitForRadiusErrorToDisappear();
        Assert.assertEquals(geofence.getVisibleErrors(), List.of(), "The error clears when the status becomes Inactive");
        Assert.assertFalse(geofence.isRadiusMarkedInvalid(), "Radius is no longer marked invalid");
        Assert.assertEquals(geofence.getRadius(), "100001", "The invalid value remains");
        Assert.assertEquals(geofence.getRadiusHelpText(), HELP_TEXT, "The help text is back");

        geofence.selectConfigStatus("Active");
        Assert.assertTrue(geofence.isRadiusRequiredMarked(), "The required marker returns when Active again");
        Assert.assertEquals(geofence.getRadius(), "100001", "The value still remains");
        Assert.assertFalse(geofence.getVisibleErrors().contains(REQUIRED), "Switching status never raises the required error");
        Assert.assertEquals(geofence.getVisibleErrors(), List.of(),
                "Validation is edit-driven, so no error reappears until Radius is edited");
    }

    @Test(groups = {"regression", "geofence"}, description = "GEO-011 Format validation applies while Inactive")
    @Story("Conditional validation")
    @Severity(SeverityLevel.CRITICAL)
    public void geo011_formatValidationWhileInactive() {
        openGeofenceSection();
        Assert.assertEquals(geofence.getConfigStatus(), "Inactive", "Precondition: Inactive");

        geofence.enterRadius("abc");

        Assert.assertEquals(geofence.getRadiusError(), WHOLE_NUMBER, "The whole-number rule applies while Inactive");
        Assert.assertTrue(geofence.isRadiusMarkedInvalid());
        Assert.assertEquals(geofence.getVisibleErrors(), List.of(WHOLE_NUMBER), "Only the format error shows: not the required one");
        Assert.assertFalse(geofence.isRadiusRequiredMarked(), "No required marker while Inactive");

        geofence.selectConfigStatus("Active");

        Assert.assertTrue(geofence.isRadiusRequiredMarked(), "The marker appears when Active");
        Assert.assertEquals(geofence.getVisibleErrors(), List.of(WHOLE_NUMBER),
                "An error raised while Inactive is still showing after switching to Active");
        Assert.assertTrue(geofence.isRadiusMarkedInvalid());
    }

    // ------------------------------------------------------------------ keyboard

    @Test(groups = {"regression", "geofence"}, description = "GEO-012 Status select opens with ArrowDown and closes with Escape")
    @Story("Geofence Config Status")
    @Severity(SeverityLevel.NORMAL)
    public void geo012_statusKeyboardBehavior() {
        openGeofenceSection();

        geofence.openConfigStatusListWithKeyboard();
        Assert.assertTrue(geofence.isConfigStatusListOpen(), "ArrowDown should open the status list");

        geofence.closeConfigStatusListWithEscape();

        Assert.assertFalse(geofence.isConfigStatusListOpen(), "Escape should close the list");
        Assert.assertEquals(geofence.getConfigStatus(), "Inactive", "The selected value is unchanged");
        Assert.assertTrue(dialog.isDialogDisplayed(), "The dialog should remain open");
    }

    // ------------------------------------------------------------------ navigation and closing

    @Test(groups = {"regression", "geofence"}, description = "GEO-013 Geofence state persists across section switches")
    @Story("Section navigation")
    @Severity(SeverityLevel.CRITICAL)
    public void geo013_stateSurvivesSectionSwitches() {
        openGeofenceSection();
        geofence.selectConfigStatus("Active");
        geofence.enterRadius("250");

        for (String other : List.of(CreateBusinessUnitDialog.BUSINESS_UNIT_DETAILS, "Currency", "ERP Integration",
                "Payment Integration", "CDP Integration", "OTP Verification")) {
            dialog.selectSection(other);
            Assert.assertEquals(dialog.getSelectedSection(), other, "Selected row after leaving Geofence for " + other);
            Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard on " + other);

            dialog.selectSection(CreateBusinessUnitGeofenceSection.SECTION);
            Assert.assertEquals(dialog.getSelectedSection(), "Geofence", "Selected row after returning from " + other);
            Assert.assertTrue(dialog.isSectionDisplayed("Geofence"), "The Geofence panel is visible again after " + other);
            Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard on return");
            Assert.assertEquals(geofence.getConfigStatus(), "Active", "Status after visiting " + other);
            Assert.assertEquals(geofence.getRadius(), "250", "Radius after visiting " + other);
        }
    }

    @Test(groups = {"regression", "geofence"}, description = "GEO-014 Reopening the dialog resets Geofence")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void geo014_reopeningResetsGeofence() {
        openGeofenceSection();
        geofence.selectConfigStatus("Active");
        geofence.enterRadius("100");

        dialog.cancel();
        Assert.assertFalse(dialog.isDialogDisplayed(), "Cancel closes the dialog");
        dashboard.clickAddBusinessUnit();
        CreateBusinessUnitDialog reopened = new CreateBusinessUnitDialog();
        reopened.waitForDialog();
        reopened.selectSection(CreateBusinessUnitGeofenceSection.SECTION);

        assertDefaultState();
        reopened.cancel();
    }

    @Test(groups = {"regression", "geofence"}, description = "GEO-015 Cancelling with Geofence values creates nothing")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void geo015_cancellingCreatesNothing() {
        dashboard = openDashboard();
        List<String> unitsBefore = dashboard.getBusinessUnitNames();
        dashboard.clickAddBusinessUnit();
        dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        dialog.selectSection(CreateBusinessUnitGeofenceSection.SECTION);
        geofence = new CreateBusinessUnitGeofenceSection();
        geofence.selectConfigStatus("Active");
        geofence.enterRadius("250");

        dialog.cancel();

        Assert.assertFalse(dialog.isDialogDisplayed(), "The dialog should be closed");
        Assert.assertEquals(dashboard.getBusinessUnitNames(), unitsBefore, "No Business Unit may have been created");
    }
}
