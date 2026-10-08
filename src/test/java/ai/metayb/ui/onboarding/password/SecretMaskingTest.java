package ai.metayb.ui.onboarding.password;

import ai.metayb.ui.wrappers.SensitiveData;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for the framework's masking rules ({@link SensitiveData}), which decide what logging, Allure steps, page
 * source and screenshots may show. They open no browser and need no credentials. The fake values below are obviously
 * fake, and no assertion message ever repeats them.
 */
@Epic("Onboarding Portal")
@Feature("Password Management - Secret masking")
public class SecretMaskingTest {

    private static final String[] SENSITIVE_NAMES = {
            "password", "currentPassword", "newPassword", "confirmPassword", "new_password", "secret", "PAYSTACK_SECRET_KEY",
            "MS_CLIENT_SECRET", "token", "accessToken", "apiKey", "api_key", "x-api-key", "accessKey", "access-key",
            "authorization", "cookie", "otp", "OTP", "one-time-code"
    };

    private static final String[] NON_SENSITIVE_NAMES = {
            "radius", "CDP_COMPANY_NAME", "name", "email", "FABRIC_DATABASE", "AMP_BUSINESS_NAME", "search", "country"
    };

    @Test(groups = {"regression", "password", "password-nologin"}, description = "A password-type field is sensitive")
    @Story("Sensitivity rules")
    @Severity(SeverityLevel.CRITICAL)
    public void passwordTypeIsSensitive() {
        Assert.assertTrue(SensitiveData.isSensitiveField("password"));
        Assert.assertTrue(SensitiveData.isSensitiveField("PASSWORD", "anything"));
    }

    @Test(groups = {"regression", "password", "password-nologin"}, description = "The same field is still sensitive once its type is text")
    @Story("Sensitivity rules")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Revealing a password flips type to text; the name, id, autocomplete, aria-label or placeholder must keep it protected.")
    public void revealedPasswordFieldStaysSensitive() {
        Assert.assertTrue(SensitiveData.isSensitiveField("text", "password"), "by name");
        Assert.assertTrue(SensitiveData.isSensitiveField("text", null, "currentPassword"), "by id");
        Assert.assertTrue(SensitiveData.isSensitiveField("text", null, null, "current-password"), "by autocomplete");
        Assert.assertTrue(SensitiveData.isSensitiveField("text", null, null, null, "Current password"), "by aria-label");
        Assert.assertTrue(SensitiveData.isSensitiveField("text", null, null, null, null, "Enter Password"), "by placeholder");
        Assert.assertTrue(SensitiveData.isSensitiveField(null, "password"), "with no type at all");
    }

    @Test(groups = {"regression", "password", "password-nologin"}, description = "Secret-looking names are sensitive; ordinary fields are not")
    @Story("Sensitivity rules")
    @Severity(SeverityLevel.CRITICAL)
    public void secretNamesAreSensitiveAndOrdinaryNamesAreNot() {
        for (String name : SENSITIVE_NAMES) {
            Assert.assertTrue(SensitiveData.isSensitiveField("text", name), name + " should be sensitive");
            Assert.assertTrue(SensitiveData.isSensitiveName(name), name + " should be a sensitive attribute name");
        }
        for (String name : NON_SENSITIVE_NAMES) {
            Assert.assertFalse(SensitiveData.isSensitiveField("text", name), name + " should not be sensitive");
        }
        Assert.assertFalse(SensitiveData.isSensitiveField("text"), "a field with no identifying attributes is not sensitive");
        Assert.assertFalse(SensitiveData.isSensitiveField(null, (String[]) null), "nothing known means not sensitive");
    }

    @Test(groups = {"regression", "password", "password-nologin"}, description = "Page source is scrubbed of sensitive values, and only those")
    @Story("Failure diagnostics")
    @Severity(SeverityLevel.CRITICAL)
    public void pageSourceIsScrubbedOfSensitiveValues() {
        String source = "<html><body><form>"
                + "<input type=\"password\" id=\"a\" placeholder=\"x\" value=\"FAKE-ONE-VALUE\">"
                + "<input type=\"text\" name=\"password\" value=\"FAKE-TWO-VALUE\">"
                + "<input id=\"b\" aria-label=\"Current password\" value='FAKE-THREE-VALUE'>"
                + "<input type=\"text\" name=\"newPassword\" value=FAKE-FOUR-VALUE>"
                + "<input value=\"FAKE-FIVE-VALUE\" type=\"text\" name=\"PAYSTACK_SECRET_KEY\"/>"
                + "<input type=\"text\" name=\"radius\" value=\"250\">"
                + "<input name=\"CDP_COMPANY_NAME\" value=\"Acme Ltd\">"
                + "<div data-value=\"keep-me\">visible text</div>"
                + "</form></body></html>";

        String scrubbed = SensitiveData.scrubPageSource(source);

        for (String fake : new String[]{"FAKE-ONE-VALUE", "FAKE-TWO-VALUE", "FAKE-THREE-VALUE", "FAKE-FOUR-VALUE", "FAKE-FIVE-VALUE"}) {
            Assert.assertFalse(scrubbed.contains(fake), "A sensitive value survived the scrub");
        }
        Assert.assertEquals(count(scrubbed, "value=\"[masked]\""), 5, "Every sensitive input should show the mask");
        Assert.assertTrue(scrubbed.contains("value=\"250\""), "Ordinary values must be kept");
        Assert.assertTrue(scrubbed.contains("value=\"Acme Ltd\""), "Ordinary values must be kept");
        Assert.assertTrue(scrubbed.contains("data-value=\"keep-me\""), "Other attributes named *value must be left alone");
        Assert.assertTrue(scrubbed.contains("visible text"), "The rest of the page must be untouched");
        Assert.assertEquals(SensitiveData.scrubPageSource(scrubbed), scrubbed, "Scrubbing twice changes nothing");
    }

    @Test(groups = {"regression", "password", "password-nologin"}, description = "Scrubbing handles empty and absent source")
    @Story("Failure diagnostics")
    @Severity(SeverityLevel.NORMAL)
    public void scrubbingToleratesEmptyInput() {
        Assert.assertNull(SensitiveData.scrubPageSource(null));
        Assert.assertEquals(SensitiveData.scrubPageSource(""), "");
        Assert.assertEquals(SensitiveData.scrubPageSource("<html><body>no inputs</body></html>"), "<html><body>no inputs</body></html>");
    }

    private static int count(String text, String part) {
        int count = 0;
        for (int at = text.indexOf(part); at >= 0; at = text.indexOf(part, at + part.length())) {
            count++;
        }
        return count;
    }
}
