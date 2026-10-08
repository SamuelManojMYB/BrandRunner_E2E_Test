package ai.metayb.ui.pages.onboarding;

import ai.metayb.config.ConfigManager;
import ai.metayb.ui.components.onboarding.DashboardPageElements;
import ai.metayb.ui.components.onboarding.VerifyPasswordElements;
import ai.metayb.ui.wrappers.BaseDriver;
import ai.metayb.ui.wrappers.SensitiveData;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The Verify Password page ({@code /verify-password}): the first step of Change Password, which asks for the current
 * password. This page object deliberately has no way to press Verify, and nothing beyond this first step is modelled:
 * submitting would be an authentication attempt on the real account.
 *
 * Password handling: the value is typed with {@code clearField} + {@code typeSecret} and is never read through the
 * logging {@code getAttribute}; emptiness is read straight from the DOM property and only "empty" or "filled" is
 * reported. The typed text is also reflected in the field's {@code value} attribute, so nothing here ever returns or
 * logs page source unscrubbed.
 */
public class VerifyPasswordPage extends BaseDriver {

    public static final String PATH = "/verify-password";
    private static final int LOAD_TIMEOUT_SECONDS = 30;

    private final VerifyPasswordElements elements;
    private final DashboardPageElements dashboardElements;

    public VerifyPasswordPage() {
        super();
        elements = new VerifyPasswordElements();
        PageFactory.initElements(getDriver(), elements);
        dashboardElements = new DashboardPageElements();
        PageFactory.initElements(getDriver(), dashboardElements);
    }

    // ---------------------------------------------------------------- reaching the page

    /** Opens the profile menu on the dashboard, clicks Change Password and waits for this page. */
    public void openThroughProfileMenu(DashboardPage dashboard) {
        dashboard.openProfileMenu();
        clickChangePasswordInOpenMenu();
    }

    /**
     * Clicks Change Password in the already open profile menu and waits for this page. The row is clicked rather than
     * its text paragraph, as for Logout: the paragraph can be covered by its own row.
     */
    public void clickChangePasswordInOpenMenu() {
        waitForElementClickable(dashboardElements.getChangePasswordLocator());
        WebElement text = getDriver().findElement(dashboardElements.getChangePasswordLocator());
        WebElement row = text.findElement(By.xpath("./ancestor::div[parent::ul[@role='menu']][1]"));
        click(row);
        waitForElementVisible(elements.getHeadingLocator(), LOAD_TIMEOUT_SECONDS);
    }

    /** Navigates straight to the route (an authenticated session stays on it, an unauthenticated one does not). */
    public void open() {
        navigateToUrl(ConfigManager.getOnboardingUiUrl().replaceAll("/+$", "") + PATH);
    }

    public void waitForPage() {
        waitForElementVisible(elements.getHeadingLocator(), LOAD_TIMEOUT_SECONDS);
    }

    /**
     * Waits for the app to move away from this route, which is what it does for a visitor without a session.
     *
     * @return true if the route changed within the timeout, false if the page stayed on {@code /verify-password}
     */
    public boolean waitForRedirectAway(int timeoutSeconds) {
        try {
            new WebDriverWait(getDriver(), Duration.ofSeconds(timeoutSeconds))
                    .until(ExpectedConditions.not(ExpectedConditions.urlContains(PATH)));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public boolean isVerifyPasswordPageDisplayed() {
        return isPresent(elements.getHeadingLocator()) && getCurrentUrl().contains(PATH);
    }

    /** The signed-in header (profile button) is present, which an unauthenticated page never shows. */
    public boolean isSignedIn() {
        return isPresent(dashboardElements.getProfileButtonLocator());
    }

    // ---------------------------------------------------------------- static content

    public String getHeading() {
        return getText(elements.getHeading()).trim();
    }

    public String getDescription() {
        return getText(elements.getDescription()).trim();
    }

    public String getPasswordName() {
        return getAttribute(elements.getPasswordInput(), "name");
    }

    public String getPasswordPlaceholder() {
        return getAttribute(elements.getPasswordInput(), "placeholder");
    }

    /** "password" while the field is masked, "text" while it is revealed. */
    public String getPasswordInputType() {
        return getAttribute(elements.getPasswordInput(), "type");
    }

    public boolean isVerifyEnabled() {
        return isEnabled(elements.getVerifyButton());
    }

    public String getVerifyButtonType() {
        return getAttribute(elements.getVerifyButton(), "type");
    }

    public String getVerifyButtonText() {
        return getText(elements.getVerifyButton()).trim();
    }

    // ---------------------------------------------------------------- password field

    /**
     * Whether the password field is empty. Read from the DOM property without logging it (the logging getAttribute is
     * never used for the value); only "empty" or "filled" is reported.
     */
    public boolean isPasswordFieldEmpty() {
        boolean empty;
        try {
            String value = elements.getPasswordInput().getDomProperty("value");
            empty = value == null || value.isEmpty();
        } catch (Exception e) {
            reportStep("Could not tell whether the password field is empty: " + e.getMessage(), "FAIL");
            return false;
        }
        reportStep("The password field is " + (empty ? "empty" : "filled"), "INFO");
        return empty;
    }

    /** Replaces the field's content with an obviously fake value, typed with typeSecret so it is never logged. */
    public void enterFakePassword(String fakePassword) {
        clearField(elements.getPasswordInput());
        typeSecret(elements.getPasswordInput(), fakePassword);
    }

    public void clearPassword() {
        clearField(elements.getPasswordInput());
    }

    public void focusPassword() {
        click(elements.getPasswordInput());
    }

    /** Moves focus away from the field by clicking the page heading. */
    public void blurPassword() {
        click(elements.getHeading());
    }

    /** Clicks the eye button, which switches the field between password and text. */
    public void toggleVisibility() {
        waitForElementClickable(elements.getEyeButtonLocator());
        click(elements.getEyeButton());
    }

    public boolean isPasswordMarkedInvalid() {
        return "true".equals(getAttribute(elements.getPasswordInput(), "aria-invalid"));
    }

    /** Validation messages currently shown on the page (none appear client side before a submit). */
    public List<String> getVisibleErrors() {
        List<String> messages = new ArrayList<>();
        for (WebElement error : elements.getErrors()) {
            messages.add(getText(error).trim());
        }
        return messages;
    }

    // ---------------------------------------------------------------- navigation

    /** Clicks the Back arrow, which leaves this page for the dashboard. */
    public void clickBack() {
        waitForElementClickable(elements.getBackButtonLocator());
        click(elements.getBackButton());
        waitForElementInvisible(elements.getHeadingLocator(), LOAD_TIMEOUT_SECONDS);
    }

    // ---------------------------------------------------------------- security checks (never return or log the value)

    /** Whether this text appears in the address bar. */
    public boolean isInUrl(String text) {
        return getDriver().getCurrentUrl().contains(text);
    }

    /** Whether this text appears in the page's visible text. Read directly so the text is never logged. */
    public boolean isShownOnPage(String text) {
        return getDriver().findElement(By.tagName("body")).getText().contains(text);
    }

    /** The page source with sensitive input values masked: the only form of page source this page object returns. */
    public String getScrubbedPageSource() {
        return SensitiveData.scrubPageSource(getDriver().getPageSource());
    }

    /** Switches revealed sensitive fields back to password (what failure diagnostics do). Returns how many changed. */
    public int maskRevealedFields() {
        return SensitiveData.maskRevealedSecrets(getDriver());
    }

    /**
     * Reads the value through the logging wrapper on purpose, to prove the wrapper masks it. Used only by the security
     * test, which then checks the captured log: the result is discarded and never exposed.
     */
    public void probeWrapperMaskingOfValue() {
        getAttribute(elements.getPasswordInput(), "value");
    }
}
