package ai.metayb.ui.pages.onboarding;

import ai.metayb.config.ConfigManager;
import ai.metayb.ui.components.onboarding.LoginPageElements;
import ai.metayb.ui.wrappers.BaseDriver;
import org.openqa.selenium.support.PageFactory;

public class LoginPage extends BaseDriver {

    private static final String LOGIN_PATH = "/login";
    private static final String FORGOT_PASSWORD_PATH = "/forgot-password";

    private final LoginPageElements elements;

    public LoginPage() {
        super();
        elements = new LoginPageElements();
        PageFactory.initElements(getDriver(), elements);
    }

    /**
     * The configured onboarding URL is the site root, which redirects to the marketing /home page,
     * so the login route is appended to it explicitly.
     */
    public void open() {
        navigateToUrl(loginUrl());
        waitForElementVisible(elements.getSignInHeadingLocator());
        waitForElementVisible(elements.getEmailInputLocator());
        if (!isLoginPageDisplayed()) {
            reportStep("The login page was not displayed after opening the login route", "FAIL");
        }
    }

    public boolean isLoginPageDisplayed() {
        return isPresent(elements.getSignInHeadingLocator())
                && isPresent(elements.getPasswordInputLocator())
                && getCurrentUrl().contains(LOGIN_PATH);
    }

    // typeSecret for both fields: type() writes the typed value to the log and the Allure report.
    public void enterEmail(String email) {
        typeSecret(elements.getEmailInput(), email);
    }

    public void enterPassword(String password) {
        typeSecret(elements.getPasswordInput(), password);
    }

    public void clickSignIn() {
        click(elements.getSignInButton());
    }

    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        clickSignIn();
    }

    public void togglePasswordVisibility() {
        click(elements.getPasswordVisibilityButton());
    }

    public boolean isPasswordVisible() {
        return "text".equals(getAttribute(elements.getPasswordInput(), "type"));
    }

    /** Waits for the email validation message to appear, then returns its text. */
    public String getEmailError() {
        waitForElementVisible(elements.getEmailErrorLocator());
        return getText(elements.getEmailError());
    }

    /** Waits for the password validation message to appear, then returns its text. */
    public String getPasswordError() {
        waitForElementVisible(elements.getPasswordErrorLocator());
        return getText(elements.getPasswordError());
    }

    public boolean isEmailErrorDisplayed() {
        return isDisplayed(elements.getEmailError());
    }

    public boolean isPasswordErrorDisplayed() {
        return isDisplayed(elements.getPasswordError());
    }

    public void waitForEmailErrorToDisappear() {
        waitForElementInvisible(elements.getEmailErrorLocator());
    }

    /** Clicks the link and waits for the destination page heading, since the route change is client-side. */
    public void clickForgotPassword() {
        click(elements.getForgotPasswordLink());
        waitForElementVisible(elements.getForgotPasswordHeadingLocator());
    }

    public boolean isForgotPasswordPageDisplayed() {
        return getCurrentUrl().contains(FORGOT_PASSWORD_PATH)
                && isPresent(elements.getForgotPasswordHeadingLocator());
    }

    private String loginUrl() {
        String base = ConfigManager.getOnboardingUiUrl().replaceAll("/+$", "");
        return base.endsWith(LOGIN_PATH) ? base : base + LOGIN_PATH;
    }
}
