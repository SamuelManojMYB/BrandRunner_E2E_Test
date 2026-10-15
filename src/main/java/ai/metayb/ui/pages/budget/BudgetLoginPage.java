package ai.metayb.ui.pages.budget;

import ai.metayb.config.ConfigManager;
import ai.metayb.ui.components.budget.BudgetLoginElements;
import ai.metayb.ui.wrappers.BaseDriver;
import org.openqa.selenium.support.PageFactory;

/** Login page of the main QA app. Both fields are typed with typeSecret, so no credential reaches the log or report. */
public class BudgetLoginPage extends BaseDriver {

    private final BudgetLoginElements elements;

    public BudgetLoginPage() {
        super();
        elements = new BudgetLoginElements();
        PageFactory.initElements(getDriver(), elements);
    }

    /** Opens the configured base URL; a signed-out browser is shown the login form there. */
    public void open() {
        navigateToUrl(ConfigManager.getBudgetUiUrl());
    }

    public void waitForLoginForm() {
        waitForElementVisible(elements.getEmailInputLocator());
    }

    public boolean isLoginFormDisplayed() {
        return isPresent(elements.getEmailInputLocator()) && isPresent(elements.getPasswordInputLocator());
    }

    public void login(String email, String password) {
        typeSecret(elements.getEmailInput(), email);
        typeSecret(elements.getPasswordInput(), password);
        click(elements.getSignInButton());
    }
}
