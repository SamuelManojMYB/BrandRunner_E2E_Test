package testUtils;

import ai.metayb.config.ConfigManager;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;

/**
 * Base class for the Budget Creation / Budget Alerts UI tests on the main QA app. Same lifecycle as
 * {@link OnboardingBaseTest} (BaseTest's configuration methods belong to no group, so TestNG would skip them under a
 * group filter; they are re-declared with alwaysRun = true), except that the browser is launched against the
 * BUDGET_UI_* configuration.
 */
public class BudgetBaseTest extends BaseTest {

    @BeforeSuite(alwaysRun = true)
    @Override
    public void onStart() {
        super.onStart();
    }

    @BeforeClass(alwaysRun = true)
    @Override
    public void setUp() {
        String url = ConfigManager.getBudgetUiUrl();
        String browser = ConfigManager.getBudgetUiBrowser();
        logger.info("[BeforeClass] - Launching {} for the Budget module: {}", browser, url);
        startApp(browser, url, ConfigManager::getBudgetUiHeadless);
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
