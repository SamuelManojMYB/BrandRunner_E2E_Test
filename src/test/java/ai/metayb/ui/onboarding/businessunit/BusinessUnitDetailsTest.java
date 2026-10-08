package ai.metayb.ui.onboarding.businessunit;

import ai.metayb.ui.pages.onboarding.CreateBusinessUnitDialog;
import ai.metayb.ui.pages.onboarding.DashboardPage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.OnboardingAuthenticatedTest;

import java.util.List;

/**
 * Business Unit Details section of the Create Business Unit dialog, covering only behavior verified on the live
 * portal. Every test opens the dialog itself from a fresh dashboard and finishes by leaving it. Nothing is
 * created: the page object has no Create or upload operation, and only unsaved form text is ever edited.
 *
 * Not covered (not safe or not yet inspected): clicking Create, uploading a logo, submit-time validation, and
 * the contents of the other six sections.
 */
@Epic("Onboarding Portal")
@Feature("Create Business Unit - Business Unit Details")
public class BusinessUnitDetailsTest extends OnboardingAuthenticatedTest {

    private static final String REQUIRED = "Business unit name is required.";
    private static final String TOO_SHORT = "At least 2 characters required.";
    private static final String TOO_LONG = "Cannot exceed 100 characters.";

    private CreateBusinessUnitDialog openCreateDialog() {
        DashboardPage dashboard = openDashboard();
        dashboard.clickAddBusinessUnit();
        CreateBusinessUnitDialog dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        return dialog;
    }

    private DashboardPage reopenFrom(DashboardPage dashboard) {
        dashboard.clickAddBusinessUnit();
        return dashboard;
    }

    // ------------------------------------------------------------------ dialog and section

    @Test(groups = {"regression", "businessunit"}, description = "Business Unit Details dialog opens correctly")
    @Story("Dialog")
    @Severity(SeverityLevel.CRITICAL)
    public void dialogOpensOnBusinessUnitDetails() {
        CreateBusinessUnitDialog dialog = openCreateDialog();

        Assert.assertTrue(dialog.isDialogDisplayed(), "Create Business Unit dialog should be displayed");
        Assert.assertTrue(dialog.getTitle().startsWith("Create Business Unit"), "Title was: " + dialog.getTitle());
        Assert.assertTrue(dialog.isBusinessUnitDetailsSelected(), "Business Unit Details should be the selected section");
        Assert.assertEquals(dialog.getSectionNames(), CreateBusinessUnitDialog.SECTIONS, "Navigation sections");
        Assert.assertEquals(dialog.getSectionHeading(), "Business Unit Details");
        Assert.assertEquals(dialog.getSectionDescription(), "Manage the name, logo and status of this business unit.");
        Assert.assertTrue(dialog.isNameFieldDisplayed(), "Business Unit Name field should be displayed");
        Assert.assertTrue(dialog.isOnDashboardRoute(), "The dialog must open over /dashboard");
        Assert.assertTrue(dialog.isCreateButtonEnabled(), "Create is enabled when the dialog opens");
    }

    @Test(groups = {"regression", "businessunit"}, description = "Name starts empty with no error")
    @Story("Business Unit Name")
    @Severity(SeverityLevel.NORMAL)
    public void nameStartsEmptyWithNoError() {
        CreateBusinessUnitDialog dialog = openCreateDialog();

        Assert.assertEquals(dialog.getBusinessUnitName(), "", "Name should start empty");
        Assert.assertFalse(dialog.isNameErrorDisplayed(), "No validation error should show initially");
    }

    @Test(groups = {"regression", "businessunit"}, description = "Untouched field does not validate on blur")
    @Story("Business Unit Name")
    @Severity(SeverityLevel.NORMAL)
    public void untouchedFieldShowsNoError() {
        CreateBusinessUnitDialog dialog = openCreateDialog();

        dialog.focusBusinessUnitName();
        dialog.blurBusinessUnitName();

        Assert.assertFalse(dialog.isNameErrorDisplayed(), "Focusing and leaving an untouched field must not show an error");
        Assert.assertEquals(dialog.getBusinessUnitName(), "");
    }

    // ------------------------------------------------------------------ name validation

    @Test(groups = {"regression", "businessunit"}, description = "Clearing an edited name shows the required error")
    @Story("Business Unit Name")
    @Severity(SeverityLevel.NORMAL)
    public void clearingEditedNameShowsRequiredError() {
        CreateBusinessUnitDialog dialog = openCreateDialog();

        dialog.appendToBusinessUnitName("ab");
        dialog.clearBusinessUnitName();

        Assert.assertEquals(dialog.getBusinessUnitName(), "", "Field should be empty");
        Assert.assertEquals(dialog.getNameError(), REQUIRED);
    }

    @Test(groups = {"regression", "businessunit"}, description = "One character is too short")
    @Story("Business Unit Name")
    @Severity(SeverityLevel.NORMAL)
    public void oneCharacterShowsMinimumLengthError() {
        CreateBusinessUnitDialog dialog = openCreateDialog();

        dialog.enterBusinessUnitName("a");

        Assert.assertEquals(dialog.getNameError(), TOO_SHORT);
    }

    @Test(groups = {"regression", "businessunit"}, description = "Two characters are accepted")
    @Story("Business Unit Name")
    @Severity(SeverityLevel.NORMAL)
    public void twoCharactersAreAccepted() {
        CreateBusinessUnitDialog dialog = openCreateDialog();
        dialog.enterBusinessUnitName("a");
        Assert.assertEquals(dialog.getNameError(), TOO_SHORT, "Precondition: one character is rejected");

        dialog.appendToBusinessUnitName("b");

        dialog.waitForNameErrorToDisappear();
        Assert.assertFalse(dialog.isNameErrorDisplayed(), "Two characters should be valid");
        Assert.assertEquals(dialog.getBusinessUnitName(), "ab");
    }

    @Test(groups = {"regression", "businessunit"}, description = "100 characters are accepted")
    @Story("Business Unit Name")
    @Severity(SeverityLevel.NORMAL)
    public void hundredCharactersAreAccepted() {
        CreateBusinessUnitDialog dialog = openCreateDialog();
        dialog.enterBusinessUnitName("a");
        Assert.assertEquals(dialog.getNameError(), TOO_SHORT, "Precondition: an error is showing");

        dialog.enterBusinessUnitName("x".repeat(100));

        dialog.waitForNameErrorToDisappear();
        Assert.assertFalse(dialog.isNameErrorDisplayed(), "100 characters should be valid");
        Assert.assertEquals(dialog.getBusinessUnitName().length(), 100);
    }

    @Test(groups = {"regression", "businessunit"}, description = "101 characters are rejected")
    @Story("Business Unit Name")
    @Severity(SeverityLevel.NORMAL)
    public void hundredAndOneCharactersShowMaximumLengthError() {
        CreateBusinessUnitDialog dialog = openCreateDialog();

        dialog.enterBusinessUnitName("x".repeat(101));

        Assert.assertEquals(dialog.getNameError(), TOO_LONG);
    }

    // ------------------------------------------------------------------ logo

    @Test(groups = {"regression", "businessunit"}, description = "Logo section and accepted file types")
    @Story("Logo")
    @Severity(SeverityLevel.NORMAL)
    @Description("Read-only: the Add Logo control is never clicked and no file is uploaded.")
    public void logoSectionAndAcceptedFileTypes() {
        CreateBusinessUnitDialog dialog = openCreateDialog();

        Assert.assertTrue(dialog.isLogoSectionDisplayed(), "Logo caption and Add Logo control should be displayed");
        Assert.assertEquals(dialog.getLogoHelpText(), "PNG, JPG · max 5 MB");
        Assert.assertEquals(dialog.getAcceptedLogoFileTypes(), "image/png,image/jpeg,image/jpg");
    }

    // ------------------------------------------------------------------ navigation

    @Test(groups = {"regression", "businessunit"}, description = "Every section can be selected")
    @Story("Section navigation")
    @Severity(SeverityLevel.CRITICAL)
    public void everySectionCanBeSelected() {
        CreateBusinessUnitDialog dialog = openCreateDialog();

        for (String section : CreateBusinessUnitDialog.SECTIONS) {
            dialog.selectSection(section);

            Assert.assertEquals(dialog.getSelectedSection(), section, "Selected section after clicking " + section);
            Assert.assertTrue(dialog.isSectionDisplayed(section), "Panel for '" + section + "' should be displayed");
            for (String other : CreateBusinessUnitDialog.SECTIONS) {
                if (!other.equals(section)) {
                    Assert.assertFalse(dialog.isSectionDisplayed(other),
                            "Panel for '" + other + "' should be hidden while '" + section + "' is selected");
                }
            }
            Assert.assertTrue(dialog.isOnDashboardRoute(), "Switching sections must not change the route");
        }
    }

    @Test(groups = {"regression", "businessunit"}, description = "Entered name persists across section switches")
    @Story("Section navigation")
    @Severity(SeverityLevel.NORMAL)
    public void nameIsKeptWhenSwitchingSections() {
        CreateBusinessUnitDialog dialog = openCreateDialog();
        dialog.enterBusinessUnitName("Persist check");

        dialog.selectSection("Currency");
        dialog.selectSection(CreateBusinessUnitDialog.BUSINESS_UNIT_DETAILS);

        Assert.assertTrue(dialog.isSectionDisplayed(CreateBusinessUnitDialog.BUSINESS_UNIT_DETAILS));
        Assert.assertEquals(dialog.getBusinessUnitName(), "Persist check", "The typed name should survive the round trip");
    }

    // ------------------------------------------------------------------ closing and reset

    @Test(groups = {"regression", "businessunit"}, description = "Cancel closes the dialog")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void cancelClosesTheDialog() {
        DashboardPage dashboard = openDashboard();
        dashboard.clickAddBusinessUnit();
        CreateBusinessUnitDialog dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();

        dialog.cancel();

        Assert.assertFalse(dialog.isDialogDisplayed(), "Dialog should be closed");
        Assert.assertTrue(dashboard.isDashboardDisplayed(), "Dashboard should remain usable");
    }

    @Test(groups = {"regression", "businessunit"}, description = "X closes the dialog")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void closeButtonClosesTheDialog() {
        DashboardPage dashboard = openDashboard();
        dashboard.clickAddBusinessUnit();
        CreateBusinessUnitDialog dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();

        dialog.closeWithX();

        Assert.assertFalse(dialog.isDialogDisplayed(), "Dialog should be closed");
        Assert.assertTrue(dashboard.isDashboardDisplayed(), "Dashboard should remain usable");
    }

    @Test(groups = {"regression", "businessunit"}, description = "Back arrow closes the dialog")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void backArrowClosesTheDialog() {
        DashboardPage dashboard = openDashboard();
        dashboard.clickAddBusinessUnit();
        CreateBusinessUnitDialog dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();

        dialog.closeWithBackArrow();

        Assert.assertFalse(dialog.isDialogDisplayed(), "Dialog should be closed");
        Assert.assertTrue(dashboard.isDashboardDisplayed(), "Dashboard should remain usable");
    }

    @Test(groups = {"regression", "businessunit"}, description = "Reopening resets unsaved values")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    public void reopeningResetsUnsavedValues() {
        DashboardPage dashboard = openDashboard();
        List<String> unitsBefore = dashboard.getBusinessUnitNames();
        dashboard.clickAddBusinessUnit();
        CreateBusinessUnitDialog dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        dialog.enterBusinessUnitName("Unsaved value");
        dialog.selectSection("Geofence");

        dialog.cancel();
        reopenFrom(dashboard);
        CreateBusinessUnitDialog reopened = new CreateBusinessUnitDialog();
        reopened.waitForDialog();

        Assert.assertTrue(reopened.isBusinessUnitDetailsSelected(), "A reopened dialog starts on Business Unit Details");
        Assert.assertEquals(reopened.getBusinessUnitName(), "", "The previous unsaved name should be gone");
        Assert.assertFalse(reopened.isNameErrorDisplayed(), "No error should carry over");
        reopened.cancel();
        Assert.assertEquals(dashboard.getBusinessUnitNames(), unitsBefore, "No Business Unit may have been created");
    }
}
