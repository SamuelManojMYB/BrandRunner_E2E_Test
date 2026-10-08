package testUtils;

import ai.metayb.ui.wrappers.BaseDriver;
import ai.metayb.ui.wrappers.SensitiveData;
import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * Convert unexpected Throwables into AssertionError so TestNG reports FAILURE instead of BROKEN in Allure.
 * Also attach screenshot + page source if a WebDriver field is present on the test instance.
 *
 * Usage:
 *  - Annotate test class: @Listeners(ConvertBrokenToFailedListener.class)
 *  - or register in testng.xml <listener class-name="com.yourcompany.listeners.ConvertBrokenToFailedListener"/>
 */
public class ConvertBrokenToFailedListener implements ITestListener {

    // The WebDriver lives in BaseDriver's ThreadLocal, not as a plain field on the test
    // instance, so fetch it directly instead of reflecting over fields.
    private WebDriver extractWebDriver(Object testInstance) {
        return BaseDriver.getDriver();
    }

    private void attachScreenshotAndSource(WebDriver driver) {
        try {
            if (driver == null) return;
            // A revealed secret would be readable in the picture: switch it back to dots first.
            SensitiveData.maskRevealedSecrets(driver);
            // Screenshot
            if (driver instanceof TakesScreenshot) {
                byte[] bytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
                try (InputStream is = new ByteArrayInputStream(bytes)) {
                    Allure.addAttachment("Screenshot", "image/png", is, ".png");
                }
            }
            // Page source
            try {
                String pageSource = driver.getPageSource();
                if (pageSource != null) {
                    // The portal reflects typed text in each input's value attribute, so the raw source of a page
                    // with a password field would contain the password. Keep the source, mask sensitive values.
                    Allure.addAttachment("Page source", "text/html", SensitiveData.scrubPageSource(pageSource));
                }
            } catch (Exception ignore) { }
        } catch (Exception ignore) { }
    }

    private void convertToFailure(ITestResult result, String reasonPrefix) {
        Throwable t = result.getThrowable();
        if (t == null) return;

        // If already an AssertionError -> it's already a failure.
        if (t instanceof AssertionError) {
            return;
        }

        // Build a new AssertionError that wraps original throwable
        AssertionError ae = new AssertionError(reasonPrefix + ": " + t.getClass().getSimpleName() + " - " + t.getMessage(), t);

        // Try to attach screenshot/page source if driver is available
        try {
            Object instance = result.getInstance();
            WebDriver driver = extractWebDriver(instance);
            attachScreenshotAndSource(driver);
        } catch (Exception ignored) { }

        // Replace throwable and mark as FAILURE
        result.setThrowable(ae);
        result.setStatus(ITestResult.FAILURE);
    }

    // Called when a test method fails
    @Override
    public void onTestFailure(ITestResult result) {
        convertToFailure(result, "Converted unexpected exception to failure (test failure)");
    }

    // Called when a configuration method (Before/After) fails
//    @Override
//    public void onConfigurationFailure(ITestResult itr) {
//        convertToFailure(itr, "Converted unexpected exception to failure (configuration failure)");
//    }

    // Other listener methods left as no-ops or small helpers
    @Override public void onTestStart(ITestResult result) { }
    @Override public void onTestSuccess(ITestResult result) { }
    @Override public void onTestSkipped(ITestResult result) { }
    @Override public void onTestFailedButWithinSuccessPercentage(ITestResult result) { }
    @Override public void onStart(ITestContext context) { }
    @Override public void onFinish(ITestContext context) { }
}
