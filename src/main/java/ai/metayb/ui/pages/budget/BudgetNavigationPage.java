package ai.metayb.ui.pages.budget;

import ai.metayb.config.ConfigManager;
import ai.metayb.ui.components.budget.BudgetElements;

import java.util.List;

/** The app shell of the main QA app: sign-in detection, the side menu and the business-unit switch. */
public class BudgetNavigationPage extends BudgetPageBase {

    /** The configured base URL without a trailing slash, so routes can be appended. */
    public static String baseUrl() {
        return ConfigManager.getBudgetUiUrl().replaceAll("/+$", "");
    }

    /** Opens the Budget list route; a signed-out browser is redirected to the login form instead. */
    public void openBudgetRoute() {
        navigateToUrl(baseUrl() + "/anp/budget");
    }

    /** True when the signed-out login form is what is on screen. */
    public boolean isLoginFormDisplayed() {
        return new BudgetLoginPage().isLoginFormDisplayed();
    }

    /** Waits for either the signed-in shell (side menu with the business unit) or the login form to render. */
    public void waitForShellOrLogin() {
        boolean rendered = waitUntil(() -> visible(BudgetElements.Nav.CURRENT_BU) || isLoginFormDisplayed(), SHELL_TIMEOUT_SECONDS);
        if (!rendered) {
            reportStep("Neither the app shell nor the login form rendered within " + SHELL_TIMEOUT_SECONDS + "s", "FAIL");
        }
    }

    /** Waits for the signed-in shell. */
    public void waitForAppShell() {
        if (!waitForVisible(BudgetElements.Nav.CURRENT_BU, SHELL_TIMEOUT_SECONDS)) {
            reportStep("The app shell (side menu) did not render within " + SHELL_TIMEOUT_SECONDS + "s", "FAIL");
        }
    }

    public boolean isAppShellDisplayed() {
        return visible(BudgetElements.Nav.CURRENT_BU);
    }

    public String currentBusinessUnit() {
        return domTextOf(BudgetElements.Nav.CURRENT_BU);
    }

    public boolean isBudgetMenuDisplayed() {
        return visible(BudgetElements.Nav.MENU_BUDGET);
    }

    /** The business units offered by the dropdown, read by opening it and closing it again without choosing. */
    public List<String> businessUnitOptions() {
        click(find(BudgetElements.Nav.CURRENT_BU));
        waitUntil(() -> count("//div[contains(@class,'MuiPaper-root')]//p[normalize-space()='" + currentBusinessUnit() + "']") > 0, UI_TIMEOUT_SECONDS);
        List<String> options = textsOf("//div[contains(@class,'MuiPaper-root')]//p[normalize-space()]");
        click(find(BudgetElements.Nav.CURRENT_BU));
        return options;
    }

    /**
     * Switches the business unit when it is not already the current one, then waits until the shell has reloaded
     * with it. The switch reloads the app, so the wait is for the new name to be shown in the header.
     */
    public void switchBusinessUnit(String name) {
        if (name.equals(currentBusinessUnit())) {
            reportStep("The business unit is already " + name, "INFO");
            return;
        }
        click(find(BudgetElements.Nav.CURRENT_BU));
        mustBeVisible(BudgetElements.Nav.buOption(name), "The business unit '" + name + "' in the dropdown");
        click(find(BudgetElements.Nav.buOption(name)));
        boolean switched = waitUntil(() -> name.equals(currentBusinessUnit()), SHELL_TIMEOUT_SECONDS);
        if (!switched) {
            reportStep("The business unit did not switch to " + name + " within " + SHELL_TIMEOUT_SECONDS + "s", "FAIL");
        }
    }

    /** Clicks Budget in the side menu and waits for the list. */
    public void openBudgetFromMenu() {
        click(find(BudgetElements.Nav.MENU_BUDGET));
        mustBeVisible(BudgetElements.ListPage.HEADING, "The Budget list");
    }
}
