package ai.metayb.ui.onboarding.authentication;

import ai.metayb.ui.pages.onboarding.LoginPage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.OnboardingBaseTest;

/**
 * Onboarding Portal login page. Covers only behavior verified on the live page: the page loading,
 * client-side validation, the password visibility toggle and the Forgot password route.
 *
 * Pending (needs onboarding credentials and unverified behavior - deliberately not implemented):
 * valid login, invalid credentials, login loading state, session behavior, and the landing page
 * (/home) "Login" link leading to /login.
 */
@Epic("Onboarding Portal")
@Feature("Authentication - Login")
public class LoginTest extends OnboardingBaseTest {

    // Non-credential values: never submitted to the backend (client-side validation stops them).
    private static final String MALFORMED_EMAIL = "not-an-email";
    private static final String VALID_SHAPED_EMAIL = "qa.probe@example.invalid";
    private static final String TEST_PASSWORD = "TestPassword123!";

    private LoginPage openLoginPage() {
        LoginPage login = new LoginPage();
        login.open();
        return login;
    }

    @Test(groups = {"smoke", "regression"}, description = "Login page loads")
    @Story("Login page")
    @Severity(SeverityLevel.CRITICAL)
    @Description("The /login route renders the Sign in heading and the credential fields.")
    public void loginPageLoads() {
        LoginPage login = openLoginPage();
        Assert.assertTrue(login.isLoginPageDisplayed(), "Login page should be displayed");
    }

    @Test(groups = "regression", description = "Empty submission shows required validation")
    @Story("Login validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Submitting the empty form shows 'Email is required' and 'Password is required' and stays on /login.")
    public void emptySubmissionShowsRequiredErrors() {
        LoginPage login = openLoginPage();

        login.clickSignIn();

        validateDisplayText(login.getEmailError(), "Email is required", "Email validation on empty submit");
        validateDisplayText(login.getPasswordError(), "Password is required", "Password validation on empty submit");
        Assert.assertTrue(login.isLoginPageDisplayed(), "Should remain on the login page");
    }

    @Test(groups = "regression", description = "Malformed email shows format validation")
    @Story("Login validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("A malformed email shows 'Please enter a valid email'.")
    public void invalidEmailFormatShowsError() {
        LoginPage login = openLoginPage();

        login.enterEmail(MALFORMED_EMAIL);
        login.clickSignIn();

        validateDisplayText(login.getEmailError(), "Please enter a valid email", "Email format validation");
    }

    @Test(groups = "regression", description = "Valid-shaped email clears the email error")
    @Story("Login validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Replacing a malformed email with a valid-shaped one clears only the email error.")
    public void validShapedEmailClearsEmailError() {
        LoginPage login = openLoginPage();

        login.enterEmail(MALFORMED_EMAIL);
        login.clickSignIn();
        validateDisplayText(login.getEmailError(), "Please enter a valid email", "Email format validation");

        login.enterEmail(VALID_SHAPED_EMAIL);
        login.waitForEmailErrorToDisappear();

        Assert.assertFalse(login.isEmailErrorDisplayed(), "Email error should be gone for a valid-shaped email");
        validateDisplayText(login.getPasswordError(), "Password is required", "Password validation is independent");
    }

    @Test(groups = "regression", description = "Password visibility toggles")
    @Story("Password visibility")
    @Severity(SeverityLevel.MINOR)
    @Description("The password input type goes password -> text -> password as the visibility button is clicked.")
    public void passwordVisibilityToggles() {
        LoginPage login = openLoginPage();
        login.enterPassword(TEST_PASSWORD);

        Assert.assertFalse(login.isPasswordVisible(), "Password should be hidden initially");

        login.togglePasswordVisibility();
        Assert.assertTrue(login.isPasswordVisible(), "Password should be visible after the first toggle");

        login.togglePasswordVisibility();
        Assert.assertFalse(login.isPasswordVisible(), "Password should be hidden after the second toggle");
    }

    @Test(groups = "regression", description = "Forgot password link navigates to /forgot-password")
    @Story("Forgot password navigation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Clicking 'Forgot password?' opens the /forgot-password route. The form is not submitted.")
    public void forgotPasswordLinkNavigates() {
        LoginPage login = openLoginPage();

        login.clickForgotPassword();

        Assert.assertTrue(login.isForgotPasswordPageDisplayed(), "Forgot password page should be displayed");
    }
}
