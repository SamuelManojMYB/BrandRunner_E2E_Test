package ai.metayb.ui.components.budget;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Locators for the login page of the main QA app (https://maggie.qas.brandrunners.ai/, the root route, which is also
 * where an unauthenticated visit to any page lands), taken from the live DOM. The form has no data-testid; its inputs
 * are found by their stable {@code name}, and the sign-in button by its submit type. The password eye is the first
 * button of the password field's row and has no label.
 */
@Getter
public class BudgetLoginElements {

    private static final String EMAIL_XPATH = "//input[@name='email']";
    private static final String PASSWORD_XPATH = "//input[@name='password']";
    private static final String SIGN_IN_XPATH = "//form//button[@type='submit']";
    private static final String WELCOME_XPATH = "//h5[normalize-space()='Welcome Back']";

    @FindBy(xpath = EMAIL_XPATH)
    private WebElement emailInput;
    private final By emailInputLocator = By.xpath(EMAIL_XPATH);

    @FindBy(xpath = PASSWORD_XPATH)
    private WebElement passwordInput;
    private final By passwordInputLocator = By.xpath(PASSWORD_XPATH);

    @FindBy(xpath = SIGN_IN_XPATH)
    private WebElement signInButton;

    private final By welcomeHeadingLocator = By.xpath(WELCOME_XPATH);
}
