package testUtils;

import ai.metayb.config.ConfigManager;
import ai.metayb.ui.budget.BudgetTestData;
import ai.metayb.ui.pages.budget.BudgetCreatePage;
import ai.metayb.ui.pages.budget.BudgetListPage;
import ai.metayb.ui.pages.budget.BudgetLoginPage;
import ai.metayb.ui.pages.budget.BudgetNavigationPage;
import org.testng.SkipException;

/**
 * For Budget tests that need the signed-in app on the Yeppie business unit. Each test calls one of the open methods,
 * which load a fresh route, sign in only when the app shows the login form (BUDGET_UI_USERNAME / BUDGET_UI_PASSWORD
 * through ConfigManager; without them the test is skipped, not failed) and make sure Yeppie is the active business
 * unit. The password is only ever passed to typeSecret and never read, logged or asserted.
 */
public class BudgetAuthenticatedTest extends BudgetBaseTest {

    /** Opens the app, signs in when needed and selects the Yeppie business unit. */
    protected BudgetNavigationPage signInAndSelectYeppie() {
        BudgetNavigationPage nav = new BudgetNavigationPage();
        nav.openBudgetRoute();
        nav.waitForShellOrLogin();
        if (nav.isLoginFormDisplayed()) {
            String username = ConfigManager.getBudgetUiUsername();
            String password = ConfigManager.getBudgetUiPassword();
            if (isBlank(username) || isBlank(password)) {
                throw new SkipException("Budget UI credentials are not configured (set BUDGET_UI_USERNAME and BUDGET_UI_PASSWORD)");
            }
            new BudgetLoginPage().login(username, password);
            nav.waitForAppShell();
        }
        nav.switchBusinessUnit(BudgetTestData.BUSINESS_UNIT);
        return nav;
    }

    protected BudgetListPage openBudgetList() {
        signInAndSelectYeppie();
        BudgetListPage list = new BudgetListPage();
        list.open();
        return list;
    }

    protected BudgetCreatePage openCreateBudget() {
        signInAndSelectYeppie();
        BudgetCreatePage create = new BudgetCreatePage();
        create.open();
        return create;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
