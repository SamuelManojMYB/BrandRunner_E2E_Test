package ai.metayb.ui.budget;

import io.qameta.allure.*;
import org.testng.SkipException;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Documented Budget Alerts / Budget Creation scenarios that cannot be automated SAFELY or UNAMBIGUOUSLY from the test
 * document. Each one is a test that is reported as SKIPPED with its reason, so nothing is silently omitted and the gap is
 * visible in every report. No assertion is invented: when the document leaves the precondition, the steps or the expected
 * behaviour open, it is not guessed.
 */
@Epic("Budget")
@Feature("Budget - Documented but not automated")
public class BudgetBlockedScenariosTest {

    @Test(groups = {"budget", "budget-blocked", "budget-alerts"}, description = "AL-010 (D-70): 'Reloading form fields for the selected location...' loader")
    @Story("Alerts")
    public void al010D70LocationLoaderCannotBeRetriggered() {
        throw new SkipException("AL-010 / D-70: the loader text is transient and the location locks after selection, so it cannot be re-triggered; "
                + "the document marks it 'Not Run' and gives no way to hold the loading state (no network interception is allowed in this suite).");
    }

    @Test(groups = {"budget", "budget-blocked", "budget-alerts"}, description = "AL-019: 'Fill all required (*) fields before saving'")
    @Story("Alerts")
    public void al019RequiredFieldsMessage() {
        throw new SkipException("AL-019 needs an activity with required (*) fields. Event Sampling has none, and the document names no other activity for this case.");
    }

    @Test(groups = {"budget", "budget-blocked", "budget-alerts"}, description = "AL-020: campaign-period date messages (invalid date ranges)")
    @Story("Alerts")
    public void al020CampaignPeriodDateMessages() {
        throw new SkipException("AL-020 needs an activity whose Campaign Period is a Date Range; the document names none. "
                + "Invalid date ranges therefore cannot be exercised, and the messages ('Enter valid dates before saving', 'Campaign period dates are required.', "
                + "'... will overflow ...') are not asserted.");
    }

    @Test(groups = {"budget", "budget-blocked", "budget-alerts"}, description = "AL-021: 'No form sections available for this activity.'")
    @Story("Alerts")
    public void al021NoFormSections() {
        throw new SkipException("AL-021 needs an activity configured with no form sections; the document names none.");
    }

    @Test(groups = {"budget", "budget-blocked", "budget-alerts"}, description = "AL-022: 'Property is already rented' (rent / property restriction)")
    @Story("Alerts")
    public void al022PropertyAlreadyRented() {
        throw new SkipException("AL-022 needs a Rent/Property activity (the document suggests 'pixel active rent') and a property code that is already in an active request. "
                + "No property code is given, and creating the active request would write additional records.");
    }

    @Test(groups = {"budget", "budget-blocked", "budget-alerts"}, description = "AL-023: upload progress and upload failure (product upload failure)")
    @Story("Alerts")
    public void al023UploadFailure() {
        throw new SkipException("AL-023 needs an activity with attachments, a file upload and a simulated network fault. The suite has no file-upload or fault-injection support, "
                + "and the document marks it 'Not Run'.");
    }

    @DataProvider(name = "messagesWithoutScenario")
    public Object[][] messagesWithoutScenario() {
        return new Object[][]{
                {"AL-040", "Failed to save activity."},
                {"AL-041", "Failed to add activity."},
                {"AL-042", "Failed to load fields for the selected location."},
                {"AL-043", "Failed to load rent reference data."},
                {"AL-044", "Failed to load History Reference data."},
                {"AL-045", "Failed to load property reference data."},
                {"AL-046", "Failed to load reference campaign data."},
                {"AL-047", "Failed to delete the activity. Please try again."},
                {"AL-048", "Save Failed – Unable to resolve activity save data. Please try again."},
                {"AL-049", "Missing Activity Data"},
                {"AL-050", "Failed to update activity"},
                {"AL-051", "Upload failed"},
                {"AL-052", "Failed to load activity fields"},
                {"AL-053", "Budget group not found"},
        };
    }

    @Test(groups = {"budget", "budget-blocked", "budget-alerts"}, dataProvider = "messagesWithoutScenario",
            description = "AL-040..AL-053: error messages the document lists without precondition, steps or expected result")
    @Story("Alerts")
    @Description("The document gives only the message text. These are server or network failure messages, so they need a forced failure that this suite cannot cause.")
    public void alErrorMessageWithoutScenario(String id, String message) {
        throw new SkipException(id + " ('" + message + "'): the document lists the message but no precondition, steps or expected result. "
                + "It is an error path that needs a forced backend/network failure, so no behaviour is invented for it.");
    }
}
