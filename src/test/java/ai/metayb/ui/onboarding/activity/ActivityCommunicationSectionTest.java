package ai.metayb.ui.onboarding.activity;

import ai.metayb.ui.pages.onboarding.CreateBusinessUnitActivitySection;
import ai.metayb.ui.pages.onboarding.CreateBusinessUnitDialog;
import ai.metayb.ui.pages.onboarding.DashboardPage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.OnboardingAuthenticatedTest;

import java.util.List;

/**
 * Activity Communication section of the Create Business Unit dialog, covering only behavior verified on the live
 * portal. Every test opens the dialog itself from a fresh dashboard and finishes by leaving it. Nothing is created:
 * Create is never clicked, and every value entered is an obviously fake placeholder that only changes unsaved form
 * state.
 *
 * The API key is a secret. It is typed with typeSecret, is never read through a logging method, never asserted and
 * never put in a message; only whether it is empty and its input type are checked, and every test that reveals it
 * also checks that the page source failure diagnostics would attach does not contain it.
 *
 * Validation is edit-driven and applies to the API key only while Status is Active; nothing here relies on submit-time
 * behavior. Sender ID is optional, so no validation is expected of it.
 */
@Epic("Onboarding Portal")
@Feature("Create Business Unit - Activity Communication")
public class ActivityCommunicationSectionTest extends OnboardingAuthenticatedTest {

    /** Obviously fake, and deliberately unlike any real key. */
    private static final String FAKE_API_KEY = "activity-key-not-real-0000";
    private static final String SENDER_ID = "sender-test-01";
    private static final String API_KEY_REQUIRED = "API key is required.";

    private DashboardPage dashboard;
    private CreateBusinessUnitDialog dialog;
    private CreateBusinessUnitActivitySection activity;

    /** Opens the dialog from a fresh dashboard and shows the Activity Communication section. */
    private void openActivitySection() {
        dashboard = openDashboard();
        dashboard.clickAddBusinessUnit();
        dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        dialog.selectSection(CreateBusinessUnitActivitySection.SECTION);
        activity = new CreateBusinessUnitActivitySection();
    }

    /** Chooses the only channel, which reveals API Key and Sender ID. */
    private void chooseChannel() {
        activity.selectChannel(CreateBusinessUnitActivitySection.INFINITY_CONNECT);
    }

    // ------------------------------------------------------------------ section and accordion

    @Test(groups = {"regression", "activity"}, description = "ACT-001 Activity Communication section opens correctly")
    @Story("Section")
    @Severity(SeverityLevel.CRITICAL)
    public void act001_sectionOpensCorrectly() {
        openActivitySection();

        Assert.assertEquals(dialog.getSelectedSection(), "Activity Communication", "Selected navigation row");
        Assert.assertEquals(activity.getActivityHeading(), "Activity Communication");
        Assert.assertEquals(activity.getActivityDescription(), "Configure how activity acknowledgements are sent.");
        Assert.assertTrue(activity.isActivitySectionDisplayed(), "The Activity Communication panel should be displayed");
        Assert.assertTrue(dialog.isSectionDisplayed("Activity Communication"), "Activity Communication should be the visible panel");
        Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard");
    }

    @Test(groups = {"regression", "activity"}, description = "ACT-002 The accordion starts expanded and collapses and expands")
    @Story("Accordion")
    @Severity(SeverityLevel.NORMAL)
    @Description("Collapsing keeps the controls in the DOM but hides them from the user, so visibility is judged by what is displayed.")
    public void act002_accordionCollapsesAndExpands() {
        openActivitySection();

        Assert.assertEquals(activity.getAccordionTitle(), "Acknowledgement Communication");
        Assert.assertEquals(activity.getAccordionDescription(), "Status, channel, and API key for acknowledgement messages.");
        Assert.assertTrue(activity.isAccordionExpanded(), "The accordion should start expanded (aria-expanded=true)");
        Assert.assertTrue(activity.isStatusDisplayed(), "Status should be visible while expanded");
        Assert.assertTrue(activity.isChannelDisplayed(), "Channel should be visible while expanded");

        activity.collapseAccordion();

        Assert.assertFalse(activity.isAccordionExpanded(), "aria-expanded should be false once collapsed");
        Assert.assertFalse(activity.isStatusDisplayed(), "Status should be hidden from the user once collapsed");
        Assert.assertFalse(activity.isChannelDisplayed(), "Channel should be hidden from the user once collapsed");
        Assert.assertEquals(activity.getAccordionTitle(), "Acknowledgement Communication", "The header stays visible");

        activity.expandAccordion();

        Assert.assertTrue(activity.isAccordionExpanded(), "aria-expanded should be true again once expanded");
        Assert.assertTrue(activity.isStatusDisplayed(), "Status should be visible again");
        Assert.assertTrue(activity.isChannelDisplayed(), "Channel should be visible again");
    }

    // ------------------------------------------------------------------ defaults and options

    @Test(groups = {"regression", "activity"}, description = "ACT-003 Default state")
    @Story("Initial state")
    @Severity(SeverityLevel.NORMAL)
    public void act003_defaultState() {
        openActivitySection();

        Assert.assertEquals(activity.getStatus(), "Inactive", "Default status");
        Assert.assertEquals(activity.getStatusValue(), "inactive", "Default status value");
        Assert.assertFalse(activity.isStatusRequiredMarked(), "Status never carries a required marker");
        Assert.assertEquals(activity.getChannel(), "", "No channel is chosen by default");
        Assert.assertEquals(activity.getChannelValue(), "", "No channel value by default");
        Assert.assertFalse(activity.isApiKeyDisplayed(), "API Key should not be visible before a channel is chosen");
        Assert.assertFalse(activity.isSenderIdDisplayed(), "Sender ID should not be visible before a channel is chosen");
        Assert.assertEquals(activity.getVisibleErrors(), List.of(), "No validation errors");
        Assert.assertFalse(activity.isAccordionErrorIconDisplayed(), "No error icon on the accordion");
    }

    @Test(groups = {"regression", "activity"}, description = "ACT-004 Status options")
    @Story("Status")
    @Severity(SeverityLevel.NORMAL)
    public void act004_statusOptions() {
        openActivitySection();

        activity.openStatusList();

        Assert.assertEquals(activity.getStatusOptions(), List.of("Active", "Inactive"), "Status options");
        Assert.assertTrue(activity.isStatusOptionSelected("Inactive"), "Inactive should be marked selected");
        Assert.assertFalse(activity.isStatusOptionSelected("Active"), "Active should not be marked selected");
        activity.closeStatusListWithEscape();
        Assert.assertEquals(activity.getStatus(), "Inactive", "The status is unchanged after viewing the options");
        Assert.assertTrue(dialog.isDialogDisplayed(), "Escape in the list must not close the dialog");
    }

    @Test(groups = {"regression", "activity"}, description = "ACT-005 Channel offers Infinity Connect")
    @Story("Channel")
    @Severity(SeverityLevel.NORMAL)
    public void act005_channelOptions() {
        openActivitySection();

        activity.openChannelList();

        Assert.assertEquals(activity.getChannelOptions(), List.of("Infinity Connect"), "Channel options");
        activity.closeChannelListWithEscape();
        Assert.assertEquals(activity.getChannel(), "", "Viewing the options chooses nothing");
        Assert.assertFalse(activity.isApiKeyDisplayed(), "Viewing the options must not reveal API Key");
    }

    @Test(groups = {"regression", "activity"}, description = "ACT-006 Choosing the channel reveals API Key and Sender ID")
    @Story("Channel")
    @Severity(SeverityLevel.CRITICAL)
    public void act006_channelRevealsApiKeyAndSenderId() {
        openActivitySection();

        chooseChannel();

        Assert.assertEquals(activity.getChannel(), "Infinity Connect");
        Assert.assertEquals(activity.getChannelValue(), CreateBusinessUnitActivitySection.INFINITY_CONNECT_VALUE, "Channel value");
        Assert.assertTrue(activity.isApiKeyDisplayed(), "API Key should become visible");
        Assert.assertTrue(activity.isSenderIdDisplayed(), "Sender ID should become visible");
        Assert.assertEquals(activity.getApiKeyInputType(), "password", "API Key is masked by default");
        Assert.assertFalse(activity.isApiKeyRequiredMarked(), "API Key is not required while Inactive (label '" + activity.getApiKeyLabel() + "')");
        Assert.assertFalse(activity.isSenderIdRequiredMarked(), "Sender ID is optional");
        Assert.assertEquals(activity.getVisibleErrors(), List.of(), "Choosing a channel shows no error");
    }

    // ------------------------------------------------------------------ required and validation

    @Test(groups = {"regression", "activity"}, description = "ACT-007 API Key is required while Active")
    @Story("API Key")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Edit-driven: the error appears when the key is edited to empty, not from switching to Active or from focus.")
    public void act007_apiKeyRequiredWhileActive() {
        openActivitySection();
        chooseChannel();

        activity.selectStatus("Active");

        Assert.assertTrue(activity.isApiKeyRequiredMarked(), "API Key should be marked required (label '" + activity.getApiKeyLabel() + "')");
        Assert.assertFalse(activity.isApiKeyErrorDisplayed(), "Switching to Active must not show an error by itself");
        activity.focusApiKey();
        activity.blurActivity();
        Assert.assertFalse(activity.isApiKeyErrorDisplayed(), "Focus and blur alone must not validate");

        activity.enterApiKey(FAKE_API_KEY);
        activity.clearApiKey();

        Assert.assertEquals(activity.getApiKeyError(), API_KEY_REQUIRED, "Error after editing the key to empty");
        Assert.assertTrue(activity.isApiKeyMarkedInvalid(), "API Key should be aria-invalid");
        Assert.assertTrue(activity.isApiKeyDescribedByItsError(), "aria-describedby should point at the error message");
        Assert.assertTrue(activity.isAccordionErrorIconDisplayed(), "The accordion header should show its error icon");

        activity.enterApiKey(FAKE_API_KEY);
        activity.waitForApiKeyErrorToDisappear();
        Assert.assertFalse(activity.isApiKeyMarkedInvalid(), "A value clears the invalid state");
        Assert.assertFalse(activity.isAccordionErrorIconDisplayed(), "The error icon goes with the error");
    }

    @Test(groups = {"regression", "activity"}, description = "ACT-008 The API Key eye toggles masking and the value stays protected")
    @Story("API Key")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Only the input type and whether the key is empty are checked; the key text is never read, asserted or logged.")
    public void act008_apiKeyVisibilityToggle() {
        openActivitySection();
        chooseChannel();
        activity.enterApiKey(FAKE_API_KEY);
        activity.enterSenderId(SENDER_ID);
        Assert.assertEquals(activity.getApiKeyInputType(), "password", "The key starts masked");

        activity.toggleApiKeyVisibility();
        Assert.assertEquals(activity.getApiKeyInputType(), "text", "The eye reveals the key");
        Assert.assertFalse(activity.isApiKeyEmpty(), "The value is kept while revealed");
        Assert.assertFalse(activity.isInDiagnosticPageSource(FAKE_API_KEY),
                "The page source that failure diagnostics attach must not contain the key, even while it is revealed");

        activity.toggleApiKeyVisibility();
        Assert.assertEquals(activity.getApiKeyInputType(), "password", "The eye masks the key again");
        Assert.assertFalse(activity.isApiKeyEmpty(), "The value is kept after masking again");
        Assert.assertEquals(activity.getSenderId(), SENDER_ID, "The Sender ID is not affected by the toggle");
    }

    @Test(groups = {"regression", "activity"}, description = "ACT-009 Active to Inactive drops the marker and error and keeps the values")
    @Story("Conditional validation")
    @Severity(SeverityLevel.CRITICAL)
    public void act009_activeToInactiveKeepsValues() {
        openActivitySection();
        chooseChannel();
        activity.selectStatus("Active");
        activity.enterApiKey(FAKE_API_KEY);
        activity.enterSenderId(SENDER_ID);
        Assert.assertTrue(activity.isApiKeyRequiredMarked(), "Precondition: API Key is required while Active");

        activity.selectStatus("Inactive");

        Assert.assertFalse(activity.isApiKeyRequiredMarked(), "The required marker disappears when Inactive");
        Assert.assertFalse(activity.isApiKeyEmpty(), "The API key is kept, not cleared by the status change");
        Assert.assertEquals(activity.getSenderId(), SENDER_ID, "The Sender ID is kept");
        Assert.assertEquals(activity.getChannel(), "Infinity Connect", "The channel is kept");
        Assert.assertEquals(activity.getVisibleErrors(), List.of(), "No errors");

        // Now with an error showing: it must go away with the status change, and the (empty) key stays as it is.
        activity.selectStatus("Active");
        activity.clearApiKey();
        Assert.assertEquals(activity.getApiKeyError(), API_KEY_REQUIRED, "Precondition: an error is showing while Active");

        activity.selectStatus("Inactive");

        activity.waitForApiKeyErrorToDisappear();
        Assert.assertFalse(activity.isApiKeyRequiredMarked(), "The required marker disappears again");
        Assert.assertFalse(activity.isApiKeyMarkedInvalid(), "The key is no longer invalid");
        Assert.assertEquals(activity.getVisibleErrors(), List.of(), "The error clears when the status becomes Inactive");
        Assert.assertFalse(activity.isAccordionErrorIconDisplayed(), "The accordion error icon clears too");
        Assert.assertEquals(activity.getSenderId(), SENDER_ID, "The Sender ID is still kept");
    }

    @Test(groups = {"regression", "activity"}, description = "ACT-010 Sender ID is optional and has no validation")
    @Story("Sender ID")
    @Severity(SeverityLevel.NORMAL)
    public void act010_senderIdIsOptional() {
        openActivitySection();
        chooseChannel();
        activity.selectStatus("Active");

        Assert.assertFalse(activity.isSenderIdRequiredMarked(), "Sender ID has no required marker even while Active");
        activity.focusSenderId();
        activity.blurActivity();
        Assert.assertFalse(activity.isSenderIdErrorDisplayed(), "An empty Sender ID shows no error on blur");

        activity.enterSenderId(SENDER_ID);
        activity.clearSenderId();

        Assert.assertEquals(activity.getSenderId(), "", "The Sender ID can be emptied");
        Assert.assertFalse(activity.isSenderIdErrorDisplayed(), "Emptying the Sender ID shows no error");
        Assert.assertFalse(activity.isSenderIdMarkedInvalid(), "The Sender ID is never invalid");
    }

    // ------------------------------------------------------------------ persistence

    @Test(groups = {"regression", "activity"}, description = "ACT-011 State persists across a round trip to Business Unit Details")
    @Story("Section navigation")
    @Severity(SeverityLevel.CRITICAL)
    public void act011_stateSurvivesRoundTrip() {
        openActivitySection();
        chooseChannel();
        activity.selectStatus("Active");
        activity.enterApiKey(FAKE_API_KEY);
        activity.enterSenderId(SENDER_ID);

        dialog.selectSection(CreateBusinessUnitDialog.BUSINESS_UNIT_DETAILS);
        dialog.selectSection(CreateBusinessUnitActivitySection.SECTION);

        verifyActivityState(true);

        activity.collapseAccordion();
        dialog.selectSection(CreateBusinessUnitDialog.BUSINESS_UNIT_DETAILS);
        dialog.selectSection(CreateBusinessUnitActivitySection.SECTION);

        Assert.assertFalse(activity.isAccordionExpanded(), "A collapsed accordion stays collapsed after a round trip");
        Assert.assertFalse(activity.isStatusDisplayed(), "The controls stay hidden while collapsed");
        activity.expandAccordion();
        verifyActivityState(true);
    }

    @Test(groups = {"regression", "activity"}, description = "ACT-012 State persists after visiting all eight sections")
    @Story("Section navigation")
    @Severity(SeverityLevel.NORMAL)
    public void act012_stateSurvivesAllSections() {
        openActivitySection();
        chooseChannel();
        activity.selectStatus("Active");
        activity.enterApiKey(FAKE_API_KEY);
        activity.enterSenderId(SENDER_ID);

        for (String section : CreateBusinessUnitDialog.SECTIONS) {
            dialog.selectSection(section);
            Assert.assertEquals(dialog.getSelectedSection(), section, "Selected row for " + section);
            Assert.assertTrue(dialog.isOnDashboardRoute(), "The route must stay /dashboard on " + section);
        }
        dialog.selectSection(CreateBusinessUnitActivitySection.SECTION);

        Assert.assertEquals(dialog.getSelectedSection(), "Activity Communication");
        verifyActivityState(true);
    }

    /** The values the tests above entered, checked without ever reading the API key's text. */
    private void verifyActivityState(boolean expanded) {
        Assert.assertEquals(activity.isAccordionExpanded(), expanded, "Accordion state");
        Assert.assertEquals(activity.getStatus(), "Active", "Status");
        Assert.assertEquals(activity.getChannel(), "Infinity Connect", "Channel");
        Assert.assertEquals(activity.getSenderId(), SENDER_ID, "Sender ID");
        Assert.assertFalse(activity.isApiKeyEmpty(), "The API key should still be filled");
        Assert.assertEquals(activity.getApiKeyInputType(), "password", "The API key should still be masked");
        Assert.assertEquals(activity.getVisibleErrors(), List.of(), "No errors appear on return");
    }
}
