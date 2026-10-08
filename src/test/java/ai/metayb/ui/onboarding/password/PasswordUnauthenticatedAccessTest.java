package ai.metayb.ui.onboarding.password;

import ai.metayb.ui.pages.onboarding.VerifyPasswordPage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.OnboardingBaseTest;

/**
 * Opening {@code /verify-password} without a session. It is its own class on purpose: the browser is launched once per
 * class with a fresh, isolated profile, so this class never signs in and never has to sign anyone out. It needs no
 * credentials and presses nothing.
 */
@Epic("Onboarding Portal")
@Feature("Password Management - Verify Password")
public class PasswordUnauthenticatedAccessTest extends OnboardingBaseTest {

    private static final int REDIRECT_TIMEOUT_SECONDS = 30;

    @Test(groups = {"regression", "password", "password-nologin"}, description = "PWD-008 Unauthenticated /verify-password redirects to login")
    @Story("Session")
    @Severity(SeverityLevel.CRITICAL)
    @Description("A visitor without a session must not see the Verify Password page; the app sends them to the login route.")
    public void pwd008_unauthenticatedAccessRedirectsToLogin() {
        VerifyPasswordPage verify = new VerifyPasswordPage();

        verify.open();

        Assert.assertTrue(verify.waitForRedirectAway(REDIRECT_TIMEOUT_SECONDS),
                "The app should leave /verify-password for a visitor without a session (stayed on " + getCurrentUrl() + ")");
        Assert.assertTrue(getCurrentUrl().contains("/login"), "Expected the login route but the URL is " + getCurrentUrl());
        Assert.assertFalse(verify.isVerifyPasswordPageDisplayed(), "The Verify Password page must not be shown");
        Assert.assertFalse(verify.isSignedIn(), "There must be no signed-in header");
    }
}
