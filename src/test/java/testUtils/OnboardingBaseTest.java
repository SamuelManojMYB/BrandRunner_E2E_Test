package testUtils;

import ai.metayb.config.ConfigManager;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;

/**
 * Base class for Onboarding Portal UI tests. Same lifecycle as {@link BaseTest} (suite setup,
 * per-method screenshot/logging, driver quit) except that the browser is launched against the
 * onboarding configuration instead of the legacy ui.* settings.
 *
 * <p>BaseTest's configuration methods belong to no group, so TestNG skips them whenever a group
 * filter is active (testng-onboarding-ui.xml selects "smoke"/"regression"). The overrides below only
 * re-declare them with alwaysRun = true so the lifecycle still runs; BaseTest itself is unchanged.
 */
public class OnboardingBaseTest extends BaseTest {

    @BeforeSuite(alwaysRun = true)
    @Override
    public void onStart() {
        super.onStart();
    }

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        String url = ConfigManager.getOnboardingUiUrl();
        String browser = ConfigManager.getOnboardingUiBrowser();
        logger.info("[BeforeClass] - Launching {} for the Onboarding Portal: {}", browser, url);
        startApp(browser, url, ConfigManager::getOnboardingUiHeadless);
    }

    @AfterMethod(alwaysRun = true)
    @Override
    public void logTestResults(ITestResult result) {
        super.logTestResults(result);
    }

    @AfterClass(alwaysRun = true)
    @Override
    public void tearDown() {
        super.tearDown();
    }
}
