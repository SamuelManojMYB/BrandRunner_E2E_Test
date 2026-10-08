package ai.metayb.ui.components.onboarding;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Locators for the Onboarding Portal login page (/login), taken from the live DOM inspection.
 * The app exposes no data-testid, and its React-generated ids (_r_N_) are not stable, so these use
 * name/href/type attributes and structural XPath. Each locator string is declared once and shared by
 * the @FindBy field and its By twin so the two cannot drift apart.
 */
@Getter
public class LoginPageElements {

    private static final String EMAIL_NAME = "email";
    private static final String PASSWORD_NAME = "password";
    private static final String PASSWORD_TOGGLE_XPATH = "//input[@name='password']/following-sibling::div//button";
    private static final String FORGOT_PASSWORD_CSS = "a[href='/forgot-password']";
    private static final String SIGN_IN_CSS = "form button[type='submit']";
    private static final String SIGN_IN_HEADING_XPATH = "//h4[normalize-space()='Sign in']";
    private static final String EMAIL_ERROR_XPATH =
            "//input[@name='email']/ancestor::div[contains(@class,'MuiFormControl-root')][1]//p[contains(@class,'Mui-error')]";
    private static final String PASSWORD_ERROR_XPATH =
            "//input[@name='password']/ancestor::div[contains(@class,'MuiFormControl-root')][1]//p[contains(@class,'Mui-error')]";
    // Heading of the page the "Forgot password?" link opens (route verification only).
    private static final String FORGOT_PASSWORD_HEADING_XPATH = "//h5[normalize-space()='Forgot Password?']";

    @FindBy(name = EMAIL_NAME)
    private WebElement emailInput;
    private final By emailInputLocator = By.name(EMAIL_NAME);

    @FindBy(name = PASSWORD_NAME)
    private WebElement passwordInput;
    private final By passwordInputLocator = By.name(PASSWORD_NAME);

    @FindBy(xpath = PASSWORD_TOGGLE_XPATH)
    private WebElement passwordVisibilityButton;

    @FindBy(css = FORGOT_PASSWORD_CSS)
    private WebElement forgotPasswordLink;

    @FindBy(css = SIGN_IN_CSS)
    private WebElement signInButton;

    private final By signInHeadingLocator = By.xpath(SIGN_IN_HEADING_XPATH);

    @FindBy(xpath = EMAIL_ERROR_XPATH)
    private WebElement emailError;
    private final By emailErrorLocator = By.xpath(EMAIL_ERROR_XPATH);

    @FindBy(xpath = PASSWORD_ERROR_XPATH)
    private WebElement passwordError;
    private final By passwordErrorLocator = By.xpath(PASSWORD_ERROR_XPATH);

    private final By forgotPasswordHeadingLocator = By.xpath(FORGOT_PASSWORD_HEADING_XPATH);
}
