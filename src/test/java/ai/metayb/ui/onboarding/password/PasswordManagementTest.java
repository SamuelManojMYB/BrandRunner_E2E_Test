package ai.metayb.ui.onboarding.password;

import ai.metayb.ui.pages.onboarding.DashboardPage;
import ai.metayb.ui.pages.onboarding.VerifyPasswordPage;
import io.qameta.allure.*;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.OnboardingAuthenticatedTest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The safe part of Password Management: the Verify Password page ({@code /verify-password}) that Change Password opens,
 * reached through the dashboard profile menu. Only local interaction with the field is automated. Nothing here presses
 * Verify, and nothing beyond this first step is touched: submitting (empty, wrong or correct) is an authentication
 * attempt on the real account, and the later steps (new password, confirmation, rules, OTP, email, reset) sit behind
 * it, so all of those are explicitly unverified.
 *
 * Only an obviously fake value is ever typed. It is entered with typeSecret, is never read through a logging method,
 * never asserted, and never put in a message; every test ends with the field masked. The unauthenticated check lives
 * in {@link PasswordUnauthenticatedAccessTest} so that it gets its own fresh browser instead of signing this one out.
 */
@Epic("Onboarding Portal")
@Feature("Password Management - Verify Password")
public class PasswordManagementTest extends OnboardingAuthenticatedTest {

    /** Obviously fake, and deliberately unlike any real password. */
    private static final String FAKE_PASSWORD = "not-a-real-password-0000";

    private DashboardPage dashboard;
    private VerifyPasswordPage verify;

    /** Opens the dashboard and reaches the Verify Password page the way a user does: profile menu, Change Password. */
    private void openVerifyPasswordPage() {
        dashboard = openDashboard();
        verify = new VerifyPasswordPage();
        verify.openThroughProfileMenu(dashboard);
    }

    // ------------------------------------------------------------------ entry point and structure

    @Test(groups = {"regression", "password"}, description = "PWD-001 Change Password opens the Verify Password page")
    @Story("Entry point")
    @Severity(SeverityLevel.CRITICAL)
    public void pwd001_changePasswordOpensVerifyPasswordPage() {
        dashboard = openDashboard();
        verify = new VerifyPasswordPage();

        dashboard.openProfileMenu();
        Assert.assertTrue(dashboard.isChangePasswordOptionDisplayed(), "Change Password should be offered in the profile menu");
        verify.clickChangePasswordInOpenMenu();

        Assert.assertTrue(verify.isVerifyPasswordPageDisplayed(), "The Verify Password page should be displayed");
        Assert.assertTrue(verify.isSignedIn(), "Opening the page must keep the session signed in");
    }

    @Test(groups = {"regression", "password"}, description = "PWD-002 Exact heading, description and password field attributes")
    @Story("Page structure")
    @Severity(SeverityLevel.NORMAL)
    public void pwd002_pageStructure() {
        openVerifyPasswordPage();

        Assert.assertEquals(verify.getHeading(), "Verify Password");
        Assert.assertEquals(verify.getDescription(), "Please enter your current password to continue changing your password.");
        Assert.assertEquals(verify.getPasswordName(), "password", "Password field name");
        Assert.assertEquals(verify.getPasswordInputType(), "password", "Password field type");
        Assert.assertEquals(verify.getPasswordPlaceholder(), "Enter Password", "Password field placeholder");
        Assert.assertEquals(verify.getVerifyButtonType(), "submit", "Verify button type");
        Assert.assertEquals(verify.getVerifyButtonText(), "Verify", "Verify button text");
    }

    @Test(groups = {"regression", "password"}, description = "PWD-003 Default state")
    @Story("Initial state")
    @Severity(SeverityLevel.NORMAL)
    public void pwd003_defaultState() {
        openVerifyPasswordPage();

        Assert.assertTrue(verify.isPasswordFieldEmpty(), "The password field should be empty");
        Assert.assertEquals(verify.getPasswordInputType(), "password", "The field should be masked");
        Assert.assertTrue(verify.isVerifyEnabled(), "Verify should be enabled");
        Assert.assertFalse(verify.isPasswordMarkedInvalid(), "The field should not be marked invalid");
        Assert.assertEquals(verify.getVisibleErrors(), List.of(), "No validation errors");
    }

    // ------------------------------------------------------------------ local field behavior (never submits)

    @Test(groups = {"regression", "password"}, description = "PWD-004 Focus, typing, clearing and blur raise no client-side error")
    @Story("Validation")
    @Severity(SeverityLevel.NORMAL)
    @Description("Observed behavior: nothing is validated on the client before a submit. Verify is never pressed.")
    public void pwd004_noClientSideErrors() {
        openVerifyPasswordPage();

        verify.focusPassword();
        verify.blurPassword();
        Assert.assertEquals(verify.getVisibleErrors(), List.of(), "Untouched focus and blur must not show an error");
        Assert.assertFalse(verify.isPasswordMarkedInvalid());

        verify.enterFakePassword(FAKE_PASSWORD);
        Assert.assertFalse(verify.isPasswordFieldEmpty(), "The fake value should have been entered");
        verify.clearPassword();
        Assert.assertTrue(verify.isPasswordFieldEmpty(), "The field should be empty after clearing");
        verify.blurPassword();

        Assert.assertEquals(verify.getVisibleErrors(), List.of(), "Clearing must not show a client-side error");
        Assert.assertFalse(verify.isPasswordMarkedInvalid());
        Assert.assertTrue(verify.isVerifyPasswordPageDisplayed(), "Nothing was submitted, so the page is unchanged");
    }

    @Test(groups = {"regression", "password"}, description = "PWD-005 The eye toggles the field between password and text")
    @Story("Show/hide")
    @Severity(SeverityLevel.NORMAL)
    @Description("Only the input type and whether the field is empty are checked; the typed text is never read or asserted.")
    public void pwd005_eyeToggle() {
        openVerifyPasswordPage();
        verify.enterFakePassword(FAKE_PASSWORD);
        Assert.assertEquals(verify.getPasswordInputType(), "password", "The field starts masked");

        verify.toggleVisibility();
        Assert.assertEquals(verify.getPasswordInputType(), "text", "The eye reveals the field");
        Assert.assertFalse(verify.isPasswordFieldEmpty(), "The value is kept while revealed");

        verify.toggleVisibility();
        Assert.assertEquals(verify.getPasswordInputType(), "password", "The eye masks the field again");
        Assert.assertFalse(verify.isPasswordFieldEmpty(), "The value is kept after masking again");
    }

    // ------------------------------------------------------------------ navigation and session

    @Test(groups = {"regression", "password"}, description = "PWD-006 Back returns to the dashboard")
    @Story("Navigation")
    @Severity(SeverityLevel.NORMAL)
    public void pwd006_backReturnsToDashboard() {
        openVerifyPasswordPage();

        verify.clickBack();

        DashboardPage back = new DashboardPage();
        back.waitForDashboardLoaded();
        Assert.assertTrue(back.isDashboardDisplayed(), "The dashboard should be displayed");
        Assert.assertTrue(getCurrentUrl().contains("/dashboard"), "The route should be /dashboard");
        Assert.assertFalse(verify.isVerifyPasswordPageDisplayed(), "The Verify Password page should be gone");
    }

    @Test(groups = {"regression", "password"}, description = "PWD-007 Direct load and refresh keep the page and the session")
    @Story("Session")
    @Severity(SeverityLevel.CRITICAL)
    public void pwd007_directLoadAndRefresh() {
        dashboard = openDashboard();
        verify = new VerifyPasswordPage();

        verify.open();
        verify.waitForPage();
        Assert.assertTrue(verify.isVerifyPasswordPageDisplayed(), "A direct load should show the page while signed in");
        Assert.assertTrue(verify.isSignedIn(), "The session should still be authenticated after a direct load");

        refresh();
        verify.waitForPage();
        Assert.assertTrue(verify.isVerifyPasswordPageDisplayed(), "A refresh should keep the page");
        Assert.assertTrue(verify.isSignedIn(), "A refresh must not sign the user out");
    }

    // ------------------------------------------------------------------ security and logging

    @Test(groups = {"regression", "password"}, description = "PWD-009 A typed password never reaches the URL, page text, logs or page source")
    @Story("Security")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Uses a fake value only. Checks the address bar, visible text, captured log output, scrubbed page source "
            + "and the failure-time re-masking, in both the masked and the revealed state. Nothing here fails on purpose.")
    public void pwd009_passwordIsNeverExposed() {
        openVerifyPasswordPage();
        CapturingAppender capture = CapturingAppender.attach();
        try {
            verify.enterFakePassword(FAKE_PASSWORD);
            Assert.assertFalse(verify.isInUrl(FAKE_PASSWORD), "The password must not be in the URL (masked)");
            Assert.assertFalse(verify.isShownOnPage(FAKE_PASSWORD), "The password must not be in the page text (masked)");

            verify.toggleVisibility();
            Assert.assertEquals(verify.getPasswordInputType(), "text", "Precondition: the field is revealed");
            Assert.assertFalse(verify.isInUrl(FAKE_PASSWORD), "The password must not be in the URL (revealed)");
            Assert.assertFalse(verify.isShownOnPage(FAKE_PASSWORD), "The password must not be in the page text (revealed)");

            // The logging wrapper is asked for the value on purpose: it must stay masked although type is now text.
            verify.probeWrapperMaskingOfValue();
            Assert.assertFalse(verify.getScrubbedPageSource().contains(FAKE_PASSWORD),
                    "The scrubbed page source (what failure diagnostics attach) must not contain the password");

            Assert.assertTrue(verify.maskRevealedFields() >= 1, "Failure diagnostics should switch the revealed field back to dots");
            Assert.assertEquals(verify.getPasswordInputType(), "password", "The field is masked again before any screenshot");
            Assert.assertFalse(verify.isPasswordFieldEmpty(), "Re-masking must not clear the value");
        } finally {
            capture.detach();
        }
        Assert.assertFalse(capture.anyMessageContains(FAKE_PASSWORD), "The password must not appear in the log output");
        Assert.assertTrue(capture.anyMessageContains("[masked]"), "The wrapper should have logged a masked value instead");
    }

    /** Collects every log line written while it is attached, so a test can prove a value never reached the log. */
    private static final class CapturingAppender extends AbstractAppender {

        private final List<String> messages = new CopyOnWriteArrayList<>();

        private CapturingAppender() {
            super("PasswordExposureCapture", null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
        }

        static CapturingAppender attach() {
            CapturingAppender appender = new CapturingAppender();
            appender.start();
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            Configuration configuration = context.getConfiguration();
            configuration.addAppender(appender);
            for (LoggerConfig loggerConfig : configuration.getLoggers().values()) {
                loggerConfig.addAppender(appender, Level.ALL, null);
            }
            context.updateLoggers();
            return appender;
        }

        void detach() {
            LoggerContext context = (LoggerContext) LogManager.getContext(false);
            for (LoggerConfig loggerConfig : context.getConfiguration().getLoggers().values()) {
                loggerConfig.removeAppender(getName());
            }
            context.updateLoggers();
            stop();
        }

        boolean anyMessageContains(String text) {
            return messages.stream().anyMatch(message -> message.contains(text));
        }

        @Override
        public void append(LogEvent event) {
            StringBuilder line = new StringBuilder(String.valueOf(event.getMessage().getFormattedMessage()));
            if (event.getThrown() != null) {
                line.append(' ').append(event.getThrown());
            }
            messages.add(line.toString());
        }
    }
}
