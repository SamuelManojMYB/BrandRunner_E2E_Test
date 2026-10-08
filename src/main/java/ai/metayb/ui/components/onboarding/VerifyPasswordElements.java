package ai.metayb.ui.components.onboarding;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Locators for the Verify Password page ({@code /verify-password}), the first step of Change Password, from the live DOM.
 *
 * The page has one password field (the current password), an eye button that shows or hides it, a Verify submit button
 * and a Back arrow button. The portal gives the field no label and gives the eye and Back buttons no aria-label or
 * title, so: the field is found by its stable {@code name}, the eye button structurally inside the field's own input
 * component, and the Back button as the button immediately followed by the visible "Back" text. The page is reached
 * through the dashboard profile menu, whose locators are {@link DashboardPageElements}' (not repeated here). Generated
 * ids are never used.
 */
@Getter
public class VerifyPasswordElements {

    public static final String PASSWORD_NAME = "password";

    private static final String HEADING_XPATH = "//h5[normalize-space()='Verify Password']";
    private static final String DESCRIPTION_XPATH =
            "//p[normalize-space()='Please enter your current password to continue changing your password.']";
    private static final String FORM_XPATH = "//form[.//input[@name='" + PASSWORD_NAME + "']]";
    private static final String PASSWORD_XPATH = "//form//input[@name='" + PASSWORD_NAME + "']";
    private static final String EYE_XPATH =
            PASSWORD_XPATH + "/ancestor::div[contains(@class,'MuiInputBase-root')][1]//button";
    private static final String VERIFY_XPATH = "//form//button[@type='submit']";
    private static final String BACK_XPATH = "//button[following-sibling::p[normalize-space()='Back']]";
    /** Any validation message the page might show: field error text in the form, or an alert. */
    private static final String ERRORS_XPATH = FORM_XPATH + "//p[contains(@class,'Mui-error')] | //*[@role='alert']";

    @FindBy(xpath = HEADING_XPATH)
    private WebElement heading;
    private final By headingLocator = By.xpath(HEADING_XPATH);

    @FindBy(xpath = DESCRIPTION_XPATH)
    private WebElement description;

    @FindBy(xpath = FORM_XPATH)
    private WebElement form;

    @FindBy(xpath = PASSWORD_XPATH)
    private WebElement passwordInput;

    @FindBy(xpath = EYE_XPATH)
    private WebElement eyeButton;
    private final By eyeButtonLocator = By.xpath(EYE_XPATH);

    @FindBy(xpath = VERIFY_XPATH)
    private WebElement verifyButton;

    @FindBy(xpath = BACK_XPATH)
    private WebElement backButton;
    private final By backButtonLocator = By.xpath(BACK_XPATH);

    @FindBy(xpath = ERRORS_XPATH)
    private List<WebElement> errors;
}
