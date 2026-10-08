package ai.metayb.ui.onboarding.crons;

import ai.metayb.ui.pages.onboarding.DashboardPage;
import ai.metayb.ui.pages.onboarding.TenantCronsDialog;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.OnboardingAuthenticatedTest;

import java.util.List;

/**
 * The Tenant Crons dialog, opened from the dashboard's "Tenant Crons" button. Strictly read-only.
 *
 * SAFETY: nothing here, and nothing in {@link TenantCronsDialog}, clicks or otherwise triggers a cron's Remove button
 * or an Add cron button. The dialog removes a cron the moment Remove is clicked (there is no confirmation), so Remove
 * is only ever checked for presence and for being enabled. The empty and error states cannot be reached safely on the
 * available tenant and are not tested.
 *
 * The dialog is opened with the existing {@link DashboardPage#openTenantCrons()}, and the cron names and statuses are
 * read with the Dashboard's own getters; each test closes the dialog it opens, and starts from a freshly loaded
 * dashboard. The currently configured cron ("AI Inference Cron", "Integrated") is the one the inspection found.
 */
@Epic("Onboarding Portal")
@Feature("Tenant Crons")
public class TenantCronsTest extends OnboardingAuthenticatedTest {

    private static final String CRON_NAME = "AI Inference Cron";
    private static final String CRON_STATUS = "Integrated";
    private static final int STAYS_OPEN_SECONDS = 3;

    private DashboardPage dashboard;
    private TenantCronsDialog crons;
    private String routeBefore;

    /** Opens a fresh dashboard and the Tenant Crons dialog, remembering the route to prove it never changes. */
    private void openTenantCrons() {
        dashboard = openDashboard();
        routeBefore = getCurrentUrl();
        dashboard.openTenantCrons();
        crons = new TenantCronsDialog();
    }

    // ------------------------------------------------------------------ the dialog

    @Test(groups = {"regression", "crons"}, description = "CRN-001 Tenant Crons dialog opens with its title, description and modal attributes")
    @Story("Dialog")
    @Severity(SeverityLevel.CRITICAL)
    public void crn001_dialogOpens() {
        openTenantCrons();

        Assert.assertTrue(dashboard.isTenantCronsDialogDisplayed(), "The Tenant Crons dialog should be displayed");
        Assert.assertEquals(crons.getHeading(), "Tenant Crons", "Dialog heading");
        Assert.assertEquals(crons.getDescription(), "Manage cron integrations at the tenant level.", "Dialog description");
        Assert.assertEquals(crons.getRole(), "dialog", "Dialog role");
        Assert.assertTrue(crons.isModal(), "The dialog should be modal (aria-modal=true)");
        Assert.assertEquals(getCurrentUrl(), routeBefore, "Opening the dialog must not change the route");
        Assert.assertTrue(getCurrentUrl().contains("/dashboard"), "The route should remain /dashboard");

        dashboard.closeTenantCrons();
    }

    @Test(groups = {"regression", "crons"}, description = "CRN-002 The cron list loads and every cron has a name and a status")
    @Story("Inventory")
    @Severity(SeverityLevel.CRITICAL)
    public void crn002_loadingAndInventory() {
        openTenantCrons();

        // The list arrives after the frame, so the loading text may or may not still be there; it is asserted when seen.
        String loading = crons.getLoadingTextIfShown();
        if (loading.isEmpty()) {
            reportStep("The loading text was gone before it could be observed", "INFO");
        } else {
            Assert.assertEquals(loading, "Loading cron details...", "Loading text");
        }

        dashboard.waitForTenantCronEntries();
        crons.waitForLoadingToFinish();
        List<String> names = dashboard.getTenantCronNames();
        List<String> statuses = dashboard.getTenantCronStatuses();

        Assert.assertFalse(names.isEmpty(), "At least one cron should be listed");
        Assert.assertEquals(statuses.size(), names.size(), "Every cron should show a status");
        for (int i = 0; i < names.size(); i++) {
            Assert.assertFalse(names.get(i).isBlank(), "Cron " + (i + 1) + " should have a name");
            Assert.assertFalse(statuses.get(i).isBlank(), "Cron '" + names.get(i) + "' should have a status");
        }

        dashboard.closeTenantCrons();
    }

    @Test(groups = {"regression", "crons"}, description = "CRN-003 The integrated cron shows its status and an enabled Remove button (never clicked)")
    @Story("Inventory")
    @Severity(SeverityLevel.NORMAL)
    @Description("Presence and enabled state only. Remove has no confirmation, so it is never clicked; Add cron is never used.")
    public void crn003_integratedCronPresentation() {
        openTenantCrons();
        dashboard.waitForTenantCronEntries();

        Assert.assertTrue(crons.isCronListed(CRON_NAME), "'" + CRON_NAME + "' should be listed");
        Assert.assertEquals(crons.getCronStatus(CRON_NAME), CRON_STATUS, "Status of '" + CRON_NAME + "'");
        Assert.assertTrue(crons.isRemoveDisplayed(CRON_NAME), "An integrated cron should offer Remove");
        Assert.assertTrue(crons.isRemoveEnabled(CRON_NAME), "Remove should be enabled");
        Assert.assertEquals(crons.getRemoveButtonText(CRON_NAME), "Remove", "Remove button text");

        dashboard.closeTenantCrons();
    }

    @Test(groups = {"regression", "crons"}, description = "CRN-004 Escape and a click outside do not close the dialog (observed behavior)")
    @Story("Closing")
    @Severity(SeverityLevel.NORMAL)
    @Description("Documents what the portal does today, not what it should do: only the Close button closes this dialog.")
    public void crn004_escapeAndBackdropDoNotClose() {
        openTenantCrons();
        dashboard.waitForTenantCronEntries();

        crons.pressEscape();
        Assert.assertTrue(crons.staysOpenFor(STAYS_OPEN_SECONDS), "Escape should not close the dialog");
        Assert.assertTrue(dashboard.isTenantCronsDialogDisplayed(), "The dialog should still be displayed after Escape");

        crons.clickBackdrop();
        Assert.assertTrue(crons.staysOpenFor(STAYS_OPEN_SECONDS), "A click outside should not close the dialog");
        Assert.assertTrue(dashboard.isTenantCronsDialogDisplayed(), "The dialog should still be displayed after clicking outside");
        Assert.assertEquals(getCurrentUrl(), routeBefore, "The route must not change");

        dashboard.closeTenantCrons();
    }

    @Test(groups = {"regression", "crons"}, description = "CRN-005 Close closes the dialog and leaves the signed-in dashboard")
    @Story("Closing")
    @Severity(SeverityLevel.CRITICAL)
    public void crn005_closeReturnsToDashboard() {
        openTenantCrons();
        dashboard.waitForTenantCronEntries();

        dashboard.closeTenantCrons();

        Assert.assertFalse(crons.isDisplayed(), "The dialog should no longer be displayed");
        Assert.assertTrue(dashboard.isDashboardDisplayed(), "The signed-in dashboard should be displayed");
        Assert.assertEquals(getCurrentUrl(), routeBefore, "The route should still be the dashboard's");
    }

    @Test(groups = {"regression", "crons"}, description = "CRN-006 A reload does not restore the dialog and keeps the session")
    @Story("Session")
    @Severity(SeverityLevel.NORMAL)
    public void crn006_reloadDoesNotRestoreDialog() {
        openTenantCrons();
        dashboard.waitForTenantCronEntries();

        refresh();
        dashboard.waitForDashboardOrLoginPage();
        Assert.assertFalse(dashboard.isLoginPageShown(), "A reload must not sign the user out");
        dashboard.waitForDashboardLoaded();

        Assert.assertFalse(crons.isDisplayed(), "The dialog must not be restored by a reload");
        Assert.assertTrue(dashboard.isDashboardDisplayed(), "The dashboard should be displayed");
    }

    @Test(groups = {"regression", "crons"}, description = "CRN-007 The dialog has no search, filter, pagination or tabs")
    @Story("Controls")
    @Severity(SeverityLevel.MINOR)
    public void crn007_unsupportedControlsAreAbsent() {
        openTenantCrons();
        dashboard.waitForTenantCronEntries();

        Assert.assertTrue(crons.isDisplayed(), "Precondition: the dialog is displayed");
        Assert.assertFalse(crons.hasSearchControl(), "There should be no search input");
        Assert.assertFalse(crons.hasFilterControl(), "There should be no filter control");
        Assert.assertFalse(crons.hasPagination(), "There should be no pagination");
        Assert.assertFalse(crons.hasTabs(), "There should be no tabs");

        dashboard.closeTenantCrons();
    }
}
