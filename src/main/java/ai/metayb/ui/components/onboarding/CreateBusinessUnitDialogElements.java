package ai.metayb.ui.components.onboarding;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Locators for the Create Business Unit dialog opened from the dashboard (route stays /dashboard), from the
 * live DOM. The dialog has no data-testid and only generated ids, so everything is anchored on the dialog's
 * role/title, visible text and stable MUI state classes (Mui-selected, Mui-error), never css-* classes.
 *
 * Every section's panel stays mounted in the DOM and only the selected one is displayed, so a section's
 * heading being present says nothing about it being shown - callers must use isDisplayed.
 */
@Getter
public class CreateBusinessUnitDialogElements {

    private static final String DIALOG = DashboardPageElements.CREATE_DIALOG_XPATH;
    private static final String NAV_ROW = DIALOG + "//div[@role='button'][contains(@class,'MuiListItemButton-root')]";

    private static final String TITLE_XPATH = DIALOG + "//h2";
    // Both header icon buttons share aria-label "Cancel & close"; the back arrow is first, the X last.
    private static final String BACK_ARROW_XPATH = "(" + DIALOG + "//button[@aria-label='Cancel & close'])[1]";
    private static final String CLOSE_X_XPATH = "(" + DIALOG + "//button[@aria-label='Cancel & close'])[last()]";
    private static final String CREATE_BUTTON_XPATH = DIALOG + "//button[normalize-space()='Create']";
    private static final String CANCEL_BUTTON_XPATH =
            CREATE_BUTTON_XPATH + "/preceding-sibling::button[normalize-space()='Cancel']";

    private static final String NAV_ROWS_XPATH = NAV_ROW;
    private static final String SELECTED_NAV_ROW_XPATH = NAV_ROW + "[contains(@class,'Mui-selected')]";

    private static final String SECTION_HEADING_XPATH = DIALOG + "//h6[normalize-space()='Business Unit Details']";
    private static final String SECTION_DESCRIPTION_XPATH =
            DIALOG + "//p[starts-with(normalize-space(),'Manage the name, logo and status')]";
    private static final String NAME_INPUT_XPATH = DIALOG + "//input[@name='name']";
    private static final String NAME_ERROR_XPATH = NAME_INPUT_XPATH
            + "/ancestor::div[contains(@class,'MuiFormControl-root')][1]//p[contains(@class,'Mui-error')]";

    private static final String LOGO_CAPTION_XPATH = DIALOG + "//p[normalize-space()='Logo']";
    private static final String ADD_LOGO_LABEL_XPATH = DIALOG + "//label[@role='button'][normalize-space()='Add Logo']";
    private static final String LOGO_FILE_INPUT_XPATH = DIALOG + "//label[normalize-space()='Add Logo']//input[@type='file']";
    private static final String LOGO_HELP_XPATH = DIALOG + "//p[contains(normalize-space(),'max 5 MB')]";

    private final By dialogLocator = By.xpath(DIALOG);

    @FindBy(xpath = TITLE_XPATH)
    private WebElement title;

    @FindBy(xpath = BACK_ARROW_XPATH)
    private WebElement backArrowButton;

    @FindBy(xpath = CLOSE_X_XPATH)
    private WebElement closeButton;

    @FindBy(xpath = CANCEL_BUTTON_XPATH)
    private WebElement cancelButton;

    // Exposed only so its enabled state can be read; there is deliberately no way to click it.
    @FindBy(xpath = CREATE_BUTTON_XPATH)
    private WebElement createButton;

    @FindBy(xpath = NAV_ROWS_XPATH)
    private List<WebElement> navRows;

    @FindBy(xpath = SELECTED_NAV_ROW_XPATH)
    private WebElement selectedNavRow;
    private final By selectedNavRowLocator = By.xpath(SELECTED_NAV_ROW_XPATH);

    @FindBy(xpath = SECTION_HEADING_XPATH)
    private WebElement sectionHeading;

    @FindBy(xpath = SECTION_DESCRIPTION_XPATH)
    private WebElement sectionDescription;

    @FindBy(xpath = NAME_INPUT_XPATH)
    private WebElement nameInput;
    private final By nameInputLocator = By.xpath(NAME_INPUT_XPATH);

    @FindBy(xpath = NAME_ERROR_XPATH)
    private WebElement nameError;
    private final By nameErrorLocator = By.xpath(NAME_ERROR_XPATH);

    @FindBy(xpath = LOGO_CAPTION_XPATH)
    private WebElement logoCaption;

    // Never click this: it opens the operating system's file picker.
    @FindBy(xpath = ADD_LOGO_LABEL_XPATH)
    private WebElement addLogoLabel;

    @FindBy(xpath = LOGO_FILE_INPUT_XPATH)
    private WebElement logoFileInput;

    @FindBy(xpath = LOGO_HELP_XPATH)
    private WebElement logoHelpText;

    // ---- locators that depend on a section name ----

    public static String navRowXpath(String section) {
        return NAV_ROW + "[.//span[normalize-space()=" + DashboardPageElements.xpathLiteral(section) + "]]";
    }

    public static By selectedNavRowFor(String section) {
        return By.xpath(navRowXpath(section) + "[contains(@class,'Mui-selected')]");
    }

    public static String sectionHeadingXpath(String section) {
        return DIALOG + "//h6[normalize-space()=" + DashboardPageElements.xpathLiteral(section) + "]";
    }
}
