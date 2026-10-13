package ai.metayb.ui.onboarding.authentication;

import ai.metayb.ui.pages.onboarding.LoginPage;
import io.qameta.allure.*;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.OnboardingBaseTest;

import java.time.Duration;

/**
 * Onboarding Portal login: server-side rejection of credentials. LoginTest covers only client-side
 * validation (it never submits to the backend); this class submits once with an account that cannot exist.
 *
 * Verified live on 2026-10-13: submitting an unregistered address with a well-formed password keeps the user
 * on /login and shows a red MUI alert reading "Email address not found." (not a field-level error), and the
 * Sign In button becomes usable again. The address uses the reserved ".invalid" TLD, so no real account is
 * ever touched or locked.
 *
 * Not automated: a WRONG password for the real onboarding account. Repeated failures against the real account
 * could lock it, and the portal's message for that case has not been observed - see the placeholder below.
 */
@Epic("Onboarding Portal")
@Feature("Authentication - Login")
public class LoginInvalidCredentialsTest extends OnboardingBaseTest {

    private static final String UNREGISTERED_EMAIL = "qa.probe@example.invalid";
    private static final String WELL_FORMED_PASSWORD = "TestPassword123!";
    private static final By ALERT_MESSAGE = By.cssSelector(".MuiAlert-message");

    @Test(groups = {"regression", "login"}, description = "Unregistered email is rejected by the server and the user stays on login")
    @Story("Invalid credentials")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Verified live: a well-formed but unregistered email shows the alert 'Email address not found.', " +
            "the URL stays on /login, no dashboard is reached, and Sign In is enabled again. No field-level " +
            "error is raised - this is a server-side result, unlike the client-side validation in LoginTest.")
    public void unregisteredEmailShowsEmailNotFoundAndStaysOnLogin() {
        LoginPage login = new LoginPage();
        login.open();
        login.login(UNREGISTERED_EMAIL, WELL_FORMED_PASSWORD);

        String alert = new WebDriverWait(getDriver(), Duration.ofSeconds(20))
                .until(ExpectedConditions.visibilityOfElementLocated(ALERT_MESSAGE)).getText().trim();

        Assert.assertEquals(alert, "Email address not found.", "Server-side rejection message");
        Assert.assertTrue(getDriver().getCurrentUrl().contains("/login"),
                "The user must stay on /login (was " + getDriver().getCurrentUrl() + ")");
        Assert.assertTrue(login.isLoginPageDisplayed(), "The sign-in form should still be displayed");
        Assert.assertFalse(login.isEmailErrorDisplayed(), "The rejection is an alert, not a field error");
        Assert.assertFalse(login.isPasswordErrorDisplayed(), "The rejection is an alert, not a field error");
        Assert.assertTrue(getDriver().findElement(By.cssSelector("form button[type='submit']")).isEnabled(),
                "Sign In should be usable again after the rejection");
    }

    @Test(enabled = false, groups = {"regression", "login"},
            description = "NOT AUTOMATED (safety): wrong password for the real onboarding account")
    @Story("Invalid credentials")
    @Description("Skipped on purpose: repeated failed sign-ins against the real onboarding account may lock it, " +
            "and the portal's message for a wrong password on a registered email has not been observed, so any " +
            "assertion would be a guess. Needs a dedicated disposable test account to automate.")
    public void wrongPasswordForRegisteredAccountNotAutomatedToAvoidLockout() {
    }
}
