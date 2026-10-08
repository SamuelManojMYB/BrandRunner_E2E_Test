package ai.metayb.ui.onboarding.dashboard;

import ai.metayb.config.ConfigManager;
import ai.metayb.ui.pages.onboarding.DashboardPage;
import ai.metayb.ui.pages.onboarding.LoginPage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import testUtils.OnboardingBaseTest;

import java.net.URI;
import java.util.List;

/**
 * Tenant Dashboard of the Onboarding Portal.
 *
 * Every test is independent: it navigates to /dashboard itself, signs in only if the app redirects to
 * the login form (credentials come from ONBOARDING_UI_USERNAME / ONBOARDING_UI_PASSWORD through
 * ConfigManager; without them authenticated tests are skipped), and finishes with every dialog and menu
 * closed. Nothing here creates, edits or deletes data: dialogs are only ever cancelled/closed, and the
 * page object has no method for Create, Update, Remove or the cron/Business Unit edit actions.
 *
 * Not covered, on purpose: Copy Application URL (the copy has no visible confirmation and the framework
 * cannot read the clipboard), Business Unit search (the dashboard has none) and opening a Business Unit
 * by clicking its card (the cards are not clickable).
 */
@Epic("Onboarding Portal")
@Feature("Tenant Dashboard")
public class DashboardTest extends OnboardingBaseTest {

    private static final List<String> CREATE_BU_SECTIONS = List.of("Business Unit Details", "Currency",
            "ERP Integration", "Payment Integration", "CDP Integration", "OTP Verification", "Geofence");
    private static final List<String> SETTINGS_SECTIONS = List.of("Organisation Details", "Timezone", "Google Maps",
            "OTP Configuration", "Email", "Invoice Configuration", "AI Settings");

    private record Snapshot(String tenantName, List<String> businessUnits, int active, int inactive) {
    }

    // ------------------------------------------------------------------ session handling

    private void signInWithConfiguredCredentials() {
        String username = ConfigManager.getOnboardingUiUsername();
        String password = ConfigManager.getOnboardingUiPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new SkipException("Onboarding credentials are not configured "
                    + "(set ONBOARDING_UI_USERNAME and ONBOARDING_UI_PASSWORD)");
        }
        new LoginPage().login(username, password);
    }

    /** Opens a freshly loaded dashboard, signing in first if the app redirects to the login form. */
    private DashboardPage openDashboard() {
        DashboardPage dashboard = new DashboardPage();
        dashboard.open();
        dashboard.waitForDashboardOrLoginPage();
        if (dashboard.isLoginPageShown()) {
            signInWithConfiguredCredentials();
        }
        dashboard.waitForDashboardLoaded();
        return dashboard;
    }

    /** Ends any existing session so a test can start from the unauthenticated state. */
    private void signOutIfSignedIn() {
        DashboardPage dashboard = new DashboardPage();
        dashboard.open();
        dashboard.waitForDashboardOrLoginPage();
        if (!dashboard.isLoginPageShown()) {
            dashboard.waitForDashboardLoaded();
            dashboard.logout();
        }
    }

    private Snapshot snapshot(DashboardPage dashboard) {
        return new Snapshot(dashboard.getTenantName(), dashboard.getBusinessUnitNames(),
                dashboard.getActiveBusinessUnitCount(), dashboard.getInactiveBusinessUnitCount());
    }

    // ------------------------------------------------------------------ shared checks (each leaves the UI clean)

    private void verifyDashboardInformation(DashboardPage dashboard) {
        Assert.assertFalse(dashboard.getTenantName().isEmpty(), "Tenant name should be shown");
        Assert.assertFalse(dashboard.getPlanName().isEmpty(), "Plan should be shown");
        Assert.assertFalse(dashboard.getUserName().isEmpty(), "User name should be shown");
        Assert.assertTrue(dashboard.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"),
                "Mail ID should look like an email address");
        Assert.assertFalse(dashboard.getHeadquarters().isEmpty(), "Headquarter should be shown");
        Assert.assertFalse(dashboard.getIndustry().isEmpty(), "Industry should be shown");
        Assert.assertTrue(dashboard.getActiveBusinessUnitCount() >= 0, "Active count should be a number");
        Assert.assertTrue(dashboard.getInactiveBusinessUnitCount() >= 0, "Inactive count should be a number");
    }

    private void verifyBusinessUnitCards(DashboardPage dashboard) {
        List<String> names = dashboard.getBusinessUnitNames();
        int activeCards = 0;
        int inactiveCards = 0;
        for (String name : names) {
            Assert.assertFalse(name.isEmpty(), "Every card should have a name");
            Assert.assertTrue(dashboard.getBusinessUnitCreatedText(name).matches("^Created on \\d{2}/\\d{2}/\\d{4}$"),
                    "Card '" + name + "' should show a created date");
            String status = dashboard.getBusinessUnitStatus(name);
            Assert.assertTrue(status.equals("Active") || status.equals("Inactive"),
                    "Card '" + name + "' should be Active or Inactive but was '" + status + "'");
            if (status.equals("Active")) {
                activeCards++;
            } else {
                inactiveCards++;
            }
        }
        Assert.assertEquals(dashboard.getActiveBusinessUnitCount(), activeCards, "Active count vs Active cards");
        Assert.assertEquals(dashboard.getInactiveBusinessUnitCount(), inactiveCards, "Inactive count vs Inactive cards");
    }

    private void verifySelectedPlanRoundTrip(DashboardPage dashboard) {
        dashboard.clickSelectedPlan();
        Assert.assertTrue(dashboard.isSelectedPlanPageDisplayed(), "Selected Plan page should be displayed");
        dashboard.returnFromSelectedPlan();
        Assert.assertTrue(dashboard.isDashboardDisplayed(), "Dashboard should be displayed after returning");
    }

    private void verifyTenantCronsDialog(DashboardPage dashboard) {
        dashboard.openTenantCrons();
        Assert.assertTrue(dashboard.isTenantCronsDialogDisplayed(), "Tenant Crons dialog should be displayed");
        dashboard.waitForTenantCronEntries();
        List<String> names = dashboard.getTenantCronNames();
        List<String> statuses = dashboard.getTenantCronStatuses();
        Assert.assertFalse(names.isEmpty(), "At least one cron should be listed");
        Assert.assertEquals(statuses.size(), names.size(), "Every cron should show a status");
        for (int i = 0; i < names.size(); i++) {
            Assert.assertFalse(names.get(i).isBlank(), "Cron " + (i + 1) + " should have a name");
            Assert.assertFalse(statuses.get(i).isBlank(), "Cron '" + names.get(i) + "' should show a status");
        }
        dashboard.closeTenantCrons();
        Assert.assertFalse(dashboard.isTenantCronsDialogDisplayed(), "Tenant Crons dialog should be closed");
        Assert.assertTrue(dashboard.isDashboardDisplayed(), "Dashboard should remain usable");
    }

    private void verifyAddBusinessUnitDialog(DashboardPage dashboard) {
        Snapshot before = snapshot(dashboard);
        dashboard.clickAddBusinessUnit();
        Assert.assertTrue(dashboard.isCreateBusinessUnitDialogDisplayed(), "Create Business Unit dialog should be displayed");
        Assert.assertTrue(dashboard.isCreateBusinessUnitNameFieldDisplayed(), "Business Unit Name field should be displayed");
        for (String section : CREATE_BU_SECTIONS) {
            Assert.assertTrue(dashboard.isCreateBusinessUnitSectionDisplayed(section), "Section '" + section + "' should be listed");
        }
        dashboard.closeCreateBusinessUnitDialog();
        Assert.assertFalse(dashboard.isCreateBusinessUnitDialogDisplayed(), "Create dialog should be closed");
        Assert.assertEquals(snapshot(dashboard), before, "Cancelling must leave the dashboard unchanged");
    }

    private void verifyDashboardSettings(DashboardPage dashboard) {
        String tenantBefore = dashboard.getTenantName();
        dashboard.openDashboardMenu();
        Assert.assertTrue(dashboard.getOpenMenuOptions().contains("Edit Settings"), "Menu should offer Edit Settings");
        dashboard.openOrganisationalSettings();
        Assert.assertTrue(dashboard.isOrganisationalSettingsDialogDisplayed(), "Organisational Settings should be displayed");
        for (String section : SETTINGS_SECTIONS) {
            Assert.assertTrue(dashboard.isOrganisationalSettingsSectionDisplayed(section), "Section '" + section + "' should be listed");
        }
        dashboard.closeOrganisationalSettings();
        Assert.assertFalse(dashboard.isOrganisationalSettingsDialogDisplayed(), "Settings dialog should be closed");
        Assert.assertEquals(dashboard.getTenantName(), tenantBefore, "Tenant name should be unchanged");
    }

    /** @return false when the tenant has no Business Unit to open a menu for (nothing was verified). */
    private boolean verifyBusinessUnitMenu(DashboardPage dashboard) {
        List<String> names = dashboard.getBusinessUnitNames();
        if (names.isEmpty()) {
            return false;
        }
        dashboard.openBusinessUnitMenu(names.get(0));
        Assert.assertEquals(dashboard.getOpenMenuOptions(), List.of("Edit Details", "Cron details"),
                "Business Unit menu options");
        dashboard.closeOpenMenu();
        return true;
    }

    private void verifyApplicationUrl(DashboardPage dashboard) {
        String text = dashboard.getApplicationUrlText();
        URI href = URI.create(dashboard.getApplicationUrlHref());
        Assert.assertFalse(text.isEmpty(), "Application URL text should be shown");
        Assert.assertTrue("https".equals(href.getScheme()) || "http".equals(href.getScheme()), "href should be an http(s) URL");
        Assert.assertNotNull(href.getHost(), "href should have a host");
        // The page shows the tenant sub-domain ("acme.qas") for the full host ("acme.qas.<domain>").
        Assert.assertTrue(href.getHost().equals(text) || href.getHost().startsWith(text + "."),
                "Displayed text '" + text + "' should be the start of the href host '" + href.getHost() + "'");
        Assert.assertTrue(dashboard.isCopyApplicationUrlControlEnabled(), "Copy control should be shown and enabled");
    }

    private void verifyProfileMenu(DashboardPage dashboard) {
        dashboard.openProfileMenu();
        Assert.assertTrue(dashboard.isChangePasswordOptionDisplayed(), "Change Password should be offered");
        Assert.assertTrue(dashboard.isLogoutOptionDisplayed(), "Logout should be offered");
        dashboard.closeOpenMenu();
    }

    // ------------------------------------------------------------------ tests

    @Test(groups = {"smoke", "regression", "dashboard"}, description = "Login and Dashboard")
    @Story("Sign in")
    @Severity(SeverityLevel.CRITICAL)
    @Description("From a signed-out state, the login page signs in and lands on a loaded dashboard.")
    public void loginAndDashboardLoad() {
        signOutIfSignedIn();
        LoginPage login = new LoginPage();
        login.open();
        signInWithConfiguredCredentials();

        DashboardPage dashboard = new DashboardPage();
        dashboard.waitForDashboardLoaded();
        Assert.assertTrue(dashboard.isDashboardDisplayed(), "Dashboard should be displayed after signing in");
        Assert.assertFalse(dashboard.getTenantName().isEmpty(), "Tenant information should be visible");
    }

    @Test(groups = {"regression", "dashboard"}, description = "Dashboard information")
    @Story("Tenant information")
    @Severity(SeverityLevel.NORMAL)
    public void dashboardInformationIsDisplayed() {
        verifyDashboardInformation(openDashboard());
    }

    @Test(groups = {"regression", "dashboard"}, description = "Business Unit cards")
    @Story("Business Units")
    @Severity(SeverityLevel.NORMAL)
    public void businessUnitCardsAreConsistent() {
        verifyBusinessUnitCards(openDashboard());
    }

    @Test(groups = {"regression", "dashboard"}, description = "Selected Plan round trip")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    public void selectedPlanNavigation() {
        verifySelectedPlanRoundTrip(openDashboard());
    }

    @Test(groups = {"regression", "dashboard"}, description = "Tenant Crons dialog")
    @Story("Tenant Crons")
    @Severity(SeverityLevel.NORMAL)
    public void tenantCronsDialog() {
        verifyTenantCronsDialog(openDashboard());
    }

    @Test(groups = {"regression", "dashboard"}, description = "Add Business Unit dialog opens and cancels")
    @Story("Add Business Unit")
    @Severity(SeverityLevel.NORMAL)
    public void addBusinessUnitDialogOpensAndCancels() {
        verifyAddBusinessUnitDialog(openDashboard());
    }

    @Test(groups = {"regression", "dashboard"}, description = "Organisational Settings dialog")
    @Story("Dashboard settings")
    @Severity(SeverityLevel.NORMAL)
    public void dashboardSettingsDialog() {
        verifyDashboardSettings(openDashboard());
    }

    @Test(groups = {"regression", "dashboard"}, description = "Business Unit three-dot menu")
    @Story("Business Units")
    @Severity(SeverityLevel.NORMAL)
    public void businessUnitMenuOptions() {
        if (!verifyBusinessUnitMenu(openDashboard())) {
            throw new SkipException("The tenant has no Business Units to open a menu for");
        }
    }

    @Test(groups = {"regression", "dashboard"}, description = "Application URL")
    @Story("Tenant information")
    @Severity(SeverityLevel.NORMAL)
    public void applicationUrl() {
        verifyApplicationUrl(openDashboard());
    }

    @Test(groups = {"regression", "dashboard"}, description = "Profile menu")
    @Story("Profile")
    @Severity(SeverityLevel.NORMAL)
    public void profileMenuOptions() {
        verifyProfileMenu(openDashboard());
    }

    @Test(groups = {"regression", "dashboard"}, description = "Refresh keeps the session")
    @Story("Session")
    @Severity(SeverityLevel.CRITICAL)
    public void refreshKeepsSession() {
        DashboardPage dashboard = openDashboard();
        String tenantBefore = dashboard.getTenantName();

        refresh();
        dashboard.waitForDashboardOrLoginPage();
        Assert.assertFalse(dashboard.isLoginPageShown(), "A refresh must not sign the user out");
        dashboard.waitForDashboardLoaded();

        Assert.assertTrue(dashboard.isDashboardDisplayed(), "Dashboard should be displayed after refresh");
        Assert.assertEquals(dashboard.getTenantName(), tenantBefore, "Tenant information should be unchanged after refresh");
    }

    @Test(groups = {"regression", "dashboard"}, description = "Unauthenticated /dashboard redirects to login")
    @Story("Session")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Opening /dashboard without a session shows the login form instead of the dashboard.")
    public void unauthenticatedDashboardRedirectsToLogin() {
        signOutIfSignedIn();

        DashboardPage dashboard = new DashboardPage();
        dashboard.open();
        dashboard.waitForDashboardOrLoginPage();

        Assert.assertTrue(dashboard.isLoginPageShown(), "Login form should be shown");
        Assert.assertTrue(getCurrentUrl().contains("/login"), "URL should be the login route");
        Assert.assertFalse(dashboard.isDashboardDisplayed(), "Dashboard must not be displayed");
    }

    @Test(groups = {"regression", "dashboard"}, description = "End-to-end: login, dashboard actions, restore, logout")
    @Story("End-to-end flow")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Sign in, validate the dashboard, exercise every read-only action leaving state unchanged, then sign out.")
    public void endToEndDashboardFlow() {
        signOutIfSignedIn();
        LoginPage login = new LoginPage();
        login.open();
        signInWithConfiguredCredentials();

        DashboardPage dashboard = new DashboardPage();
        dashboard.waitForDashboardLoaded();
        Assert.assertTrue(dashboard.isDashboardDisplayed(), "Dashboard should be displayed after signing in");
        Snapshot start = snapshot(dashboard);

        verifyDashboardInformation(dashboard);
        verifyBusinessUnitCards(dashboard);
        verifyApplicationUrl(dashboard);
        verifySelectedPlanRoundTrip(dashboard);
        verifyTenantCronsDialog(dashboard);
        verifyAddBusinessUnitDialog(dashboard);
        verifyDashboardSettings(dashboard);
        verifyBusinessUnitMenu(dashboard);
        verifyProfileMenu(dashboard);
        Assert.assertEquals(snapshot(dashboard), start, "The dashboard must be unchanged after the actions");

        dashboard.logout();
        Assert.assertTrue(dashboard.isLoginPageShown(), "Logout should return to the login form");
    }
}
