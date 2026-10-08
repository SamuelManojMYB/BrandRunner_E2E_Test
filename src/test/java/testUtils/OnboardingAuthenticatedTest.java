package testUtils;

import ai.metayb.config.ConfigManager;
import ai.metayb.ui.pages.onboarding.DashboardPage;
import ai.metayb.ui.pages.onboarding.LoginPage;
import org.testng.SkipException;

/**
 * For onboarding tests that need a signed-in dashboard: each test calls {@link #openDashboard()}, which loads a
 * fresh /dashboard and signs in with the configured onboarding account only if the app redirects to the login
 * form. Credentials come from ConfigManager (ONBOARDING_UI_USERNAME / ONBOARDING_UI_PASSWORD); without them the
 * test is skipped rather than failed.
 */
public class OnboardingAuthenticatedTest extends OnboardingBaseTest {

    protected void signInWithConfiguredCredentials() {
        String username = ConfigManager.getOnboardingUiUsername();
        String password = ConfigManager.getOnboardingUiPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new SkipException("Onboarding credentials are not configured "
                    + "(set ONBOARDING_UI_USERNAME and ONBOARDING_UI_PASSWORD)");
        }
        new LoginPage().login(username, password);
    }

    /** Opens a freshly loaded, fully rendered dashboard, signing in first if needed. */
    protected DashboardPage openDashboard() {
        DashboardPage dashboard = new DashboardPage();
        dashboard.open();
        dashboard.waitForDashboardOrLoginPage();
        if (dashboard.isLoginPageShown()) {
            signInWithConfiguredCredentials();
        }
        dashboard.waitForDashboardLoaded();
        return dashboard;
    }
}
