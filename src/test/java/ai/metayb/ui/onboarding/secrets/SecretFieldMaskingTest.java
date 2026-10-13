package ai.metayb.ui.onboarding.secrets;

import ai.metayb.ui.components.onboarding.ErpField;
import ai.metayb.ui.pages.onboarding.*;
import ai.metayb.ui.wrappers.SensitiveData;
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
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.OnboardingAuthenticatedTest;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Secret fields of the Create Business Unit dialog (ERP MS Client Secret, Payment secret key, Activity
 * Communication API Key): the entered value stays masked and never leaks into the URL, the visible page text, the
 * scrubbed page source that failure diagnostics attach, or the log output - after entry, after moving between
 * sections, after a page refresh, and after cancelling and reopening the dialog.
 *
 * Nothing is saved: Create is never clicked, every value is an obviously fake placeholder, and the dialog is
 * always cancelled. Complements SecretMaskingTest (the masking utilities themselves) and PasswordManagementTest
 * PWD-009 (the Verify Password page).
 */
@Epic("Onboarding Portal")
@Feature("Secret Masking")
public class SecretFieldMaskingTest extends OnboardingAuthenticatedTest {

    private static final String FAKE_ERP_SECRET = "fake-erp-secret-9f3a1c";
    private static final String FAKE_PAYMENT_SECRET = "fake-payment-secret-4d7b2e";
    private static final String FAKE_API_KEY = "fake-api-key-8e5c0a";

    private CreateBusinessUnitDialog openDialog() {
        DashboardPage dashboard = openDashboard();
        dashboard.clickAddBusinessUnit();
        CreateBusinessUnitDialog dialog = new CreateBusinessUnitDialog();
        dialog.waitForDialog();
        return dialog;
    }

    private void assertNotExposed(String secret, String where, CapturingAppender capture) {
        Assert.assertFalse(getDriver().getCurrentUrl().contains(secret), "Secret must not be in the URL " + where);
        Assert.assertFalse(getDriver().findElement(By.tagName("body")).getText().contains(secret),
                "Secret must not be in the visible page text " + where);
        Assert.assertFalse(SensitiveData.scrubPageSource(getDriver().getPageSource()).contains(secret),
                "Secret must not be in the scrubbed page source " + where);
        if (capture != null) {
            Assert.assertFalse(capture.anyMessageContains(secret), "Secret must not be in the log output " + where);
        }
    }

    @Test(groups = {"regression", "secrets"}, description = "ERP client secret stays masked across entry, navigation, refresh and reopening")
    @Story("ERP secret")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Entering a fake MS Client Secret keeps the field type=password; the value never appears in the " +
            "URL, page text, scrubbed page source or logs; it is still masked after switching sections and back; " +
            "a refresh leaves no trace of it; and a reopened dialog starts with the field empty and still masked.")
    public void erpClientSecretStaysMasked() {
        CapturingAppender capture = CapturingAppender.attach();
        try {
            CreateBusinessUnitDialog dialog = openDialog();
            dialog.selectSection(CreateBusinessUnitErpSection.SECTION);
            CreateBusinessUnitErpSection erp = new CreateBusinessUnitErpSection();
            erp.selectConfigStatus(CreateBusinessUnitErpSection.ACTIVE);
            erp.enterFieldValue(ErpField.CLIENT_SECRET, FAKE_ERP_SECRET);

            Assert.assertEquals(erp.getFieldInputType(ErpField.CLIENT_SECRET), "password", "Masked after entry");
            assertNotExposed(FAKE_ERP_SECRET, "after entry", capture);

            dialog.selectSection("Currency");
            dialog.selectSection(CreateBusinessUnitErpSection.SECTION);
            Assert.assertEquals(erp.getFieldInputType(ErpField.CLIENT_SECRET), "password", "Masked after navigation");
            Assert.assertFalse(erp.isFieldEmpty(ErpField.CLIENT_SECRET), "The entered value is kept (masked) after navigation");
            assertNotExposed(FAKE_ERP_SECRET, "after navigation", capture);

            refresh();
            openDashboard();
            assertNotExposed(FAKE_ERP_SECRET, "after refresh", capture);

            dialog = openDialog();
            dialog.selectSection(CreateBusinessUnitErpSection.SECTION);
            erp = new CreateBusinessUnitErpSection();
            Assert.assertEquals(erp.getFieldInputType(ErpField.CLIENT_SECRET), "password", "Masked after reopening");
            Assert.assertTrue(erp.isFieldEmpty(ErpField.CLIENT_SECRET), "A reopened dialog must not retain the secret");
            dialog.cancel();
        } finally {
            capture.detach();
        }
    }

    @Test(groups = {"regression", "secrets"}, description = "Payment secret key stays masked across entry, navigation, refresh and reopening")
    @Story("Payment secret")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Same guarantees as the ERP secret, for the Payment Integration secret key.")
    public void paymentSecretKeyStaysMasked() {
        CapturingAppender capture = CapturingAppender.attach();
        try {
            CreateBusinessUnitDialog dialog = openDialog();
            dialog.selectSection(CreateBusinessUnitPaymentSection.SECTION);
            CreateBusinessUnitPaymentSection payment = new CreateBusinessUnitPaymentSection();
            payment.selectConfigStatus(CreateBusinessUnitPaymentSection.ACTIVE);
            payment.enterSecretKey(FAKE_PAYMENT_SECRET);

            Assert.assertEquals(payment.getSecretKeyInputType(), "password", "Masked after entry");
            assertNotExposed(FAKE_PAYMENT_SECRET, "after entry", capture);

            dialog.selectSection("Currency");
            dialog.selectSection(CreateBusinessUnitPaymentSection.SECTION);
            Assert.assertEquals(payment.getSecretKeyInputType(), "password", "Masked after navigation");
            Assert.assertFalse(payment.isSecretKeyEmpty(), "The entered value is kept (masked) after navigation");
            assertNotExposed(FAKE_PAYMENT_SECRET, "after navigation", capture);

            refresh();
            openDashboard();
            assertNotExposed(FAKE_PAYMENT_SECRET, "after refresh", capture);

            dialog = openDialog();
            dialog.selectSection(CreateBusinessUnitPaymentSection.SECTION);
            payment = new CreateBusinessUnitPaymentSection();
            Assert.assertEquals(payment.getSecretKeyInputType(), "password", "Masked after reopening");
            Assert.assertTrue(payment.isSecretKeyEmpty(), "A reopened dialog must not retain the secret");
            dialog.cancel();
        } finally {
            capture.detach();
        }
    }

    @Test(groups = {"regression", "secrets"}, description = "Activity Communication API key stays masked, also while revealed")
    @Story("Activity API key")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Same guarantees as the ERP secret, for the API Key; additionally the value stays out of the page " +
            "text and scrubbed page source while the eye toggle has revealed it (type=text), and is masked again " +
            "after toggling back.")
    public void activityApiKeyStaysMasked() {
        CapturingAppender capture = CapturingAppender.attach();
        try {
            CreateBusinessUnitDialog dialog = openDialog();
            dialog.selectSection(CreateBusinessUnitActivitySection.SECTION);
            CreateBusinessUnitActivitySection activity = new CreateBusinessUnitActivitySection();
            activity.selectStatus(CreateBusinessUnitActivitySection.ACTIVE);
            activity.selectChannel(CreateBusinessUnitActivitySection.INFINITY_CONNECT);
            activity.enterApiKey(FAKE_API_KEY);

            Assert.assertEquals(activity.getApiKeyInputType(), "password", "Masked after entry");
            assertNotExposed(FAKE_API_KEY, "after entry", capture);

            activity.toggleApiKeyVisibility();
            Assert.assertEquals(activity.getApiKeyInputType(), "text", "Precondition: the eye reveals the field");
            assertNotExposed(FAKE_API_KEY, "while revealed", capture);
            activity.toggleApiKeyVisibility();
            Assert.assertEquals(activity.getApiKeyInputType(), "password", "Masked again after toggling back");

            dialog.selectSection("Currency");
            dialog.selectSection(CreateBusinessUnitActivitySection.SECTION);
            Assert.assertEquals(activity.getApiKeyInputType(), "password", "Masked after navigation");
            assertNotExposed(FAKE_API_KEY, "after navigation", capture);

            refresh();
            openDashboard();
            assertNotExposed(FAKE_API_KEY, "after refresh", capture);

            dialog = openDialog();
            dialog.selectSection(CreateBusinessUnitActivitySection.SECTION);
            activity = new CreateBusinessUnitActivitySection();
            activity.selectStatus(CreateBusinessUnitActivitySection.ACTIVE);
            activity.selectChannel(CreateBusinessUnitActivitySection.INFINITY_CONNECT);
            Assert.assertTrue(activity.isApiKeyEmpty(), "A reopened dialog must not retain the API key");
            Assert.assertEquals(activity.getApiKeyInputType(), "password", "Masked after reopening");
            dialog.cancel();
        } finally {
            capture.detach();
        }
    }

    /** Collects every log line written while attached, so a test can prove a value never reached the log. */
    private static final class CapturingAppender extends AbstractAppender {

        private final List<String> messages = new CopyOnWriteArrayList<>();

        private CapturingAppender() {
            super("SecretFieldMaskingCapture", null, PatternLayout.createDefaultLayout(), true, Property.EMPTY_ARRAY);
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
