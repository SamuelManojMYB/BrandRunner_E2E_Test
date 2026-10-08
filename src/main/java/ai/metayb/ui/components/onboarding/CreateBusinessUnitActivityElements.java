package ai.metayb.ui.components.onboarding;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Locators for the Activity Communication section of the Create Business Unit dialog, from the live DOM.
 *
 * The section holds one accordion, "Acknowledgement Communication", whose header is a button with aria-expanded and
 * whose body holds four controls found by their stable {@code name}: Status ({@code status}) and Channel
 * ({@code channel}) are MUI Selects, and API Key ({@code api_key}, a password input with an eye button) and Sender ID
 * ({@code senderId}) appear once a channel is chosen. Collapsing the accordion keeps the controls in the DOM but makes
 * them {@code visibility: hidden}, so "hidden from the user" is the displayed state, never DOM presence. Every Create
 * Business Unit panel stays mounted, so everything is scoped to this panel through its heading. The API Key label gains
 * a trailing " *" while Status is Active. When the section has an error, the accordion header shows an extra icon that
 * has no label or test id; it is the direct svg of the header's first span (absent without an error). No generated ids.
 */
@Getter
public class CreateBusinessUnitActivityElements {

    public static final String STATUS_NAME = "status";
    public static final String CHANNEL_NAME = "channel";
    public static final String API_KEY_NAME = "api_key";
    public static final String SENDER_ID_NAME = "senderId";

    private static final String DIALOG = DashboardPageElements.CREATE_DIALOG_XPATH;
    private static final String FORM_CONTROL = "/ancestor::div[contains(@class,'MuiFormControl-root')][1]";
    private static final String INPUT_BASE = "/ancestor::div[contains(@class,'MuiInputBase-root')][1]";

    private static final String HEADING_XPATH = DIALOG + "//h6[normalize-space()='Activity Communication']";
    /** The panel: the box that holds the heading, the description and the accordion. */
    private static final String PANEL = HEADING_XPATH + "/parent::div";
    private static final String DESCRIPTION_XPATH = PANEL + "/p[normalize-space()='Configure how activity acknowledgements are sent.']";

    // ---- accordion ----
    private static final String ACCORDION_HEADER_XPATH = PANEL + "//button[@aria-expanded]";
    private static final String ACCORDION_TITLE_XPATH = ACCORDION_HEADER_XPATH + "//h6[normalize-space()='Acknowledgement Communication']";
    private static final String ACCORDION_DESCRIPTION_XPATH =
            ACCORDION_HEADER_XPATH + "//span[normalize-space()='Status, channel, and API key for acknowledgement messages.']";
    // svg elements are in the SVG namespace, so they are matched by local-name(), not by a plain "svg" name test.
    private static final String ACCORDION_ERROR_ICON_XPATH = ACCORDION_HEADER_XPATH + "/span[1]/*[local-name()='svg']";

    // ---- Status and Channel (MUI Selects, found through their hidden named input) ----
    private static final String STATUS_INPUT_XPATH = PANEL + "//input[@name='" + STATUS_NAME + "']";
    private static final String STATUS_SELECT_XPATH = STATUS_INPUT_XPATH + INPUT_BASE + "//div[@role='combobox']";
    private static final String STATUS_LABEL_XPATH = STATUS_INPUT_XPATH + FORM_CONTROL + "//label";
    private static final String CHANNEL_INPUT_XPATH = PANEL + "//input[@name='" + CHANNEL_NAME + "']";
    private static final String CHANNEL_SELECT_XPATH = CHANNEL_INPUT_XPATH + INPUT_BASE + "//div[@role='combobox']";
    private static final String CHANNEL_LABEL_XPATH = CHANNEL_INPUT_XPATH + FORM_CONTROL + "//label";

    // ---- API Key ----
    private static final String API_KEY_INPUT_XPATH = PANEL + "//input[@name='" + API_KEY_NAME + "']";
    private static final String API_KEY_LABEL_XPATH = API_KEY_INPUT_XPATH + FORM_CONTROL + "//label";
    private static final String API_KEY_MESSAGE_XPATH = API_KEY_INPUT_XPATH + FORM_CONTROL + "//p[contains(@class,'MuiFormHelperText-root')]";
    private static final String API_KEY_ERROR_XPATH = API_KEY_INPUT_XPATH + FORM_CONTROL + "//p[contains(@class,'Mui-error')]";
    private static final String API_KEY_EYE_XPATH = API_KEY_INPUT_XPATH + INPUT_BASE + "//button";

    // ---- Sender ID ----
    private static final String SENDER_ID_INPUT_XPATH = PANEL + "//input[@name='" + SENDER_ID_NAME + "']";
    private static final String SENDER_ID_LABEL_XPATH = SENDER_ID_INPUT_XPATH + FORM_CONTROL + "//label";
    private static final String SENDER_ID_ERROR_XPATH = SENDER_ID_INPUT_XPATH + FORM_CONTROL + "//p[contains(@class,'Mui-error')]";

    // ---- option lists rendered by MUI outside the dialog ----
    private static final String LISTBOX_XPATH = "//ul[@role='listbox']";
    private static final String OPTIONS_XPATH = LISTBOX_XPATH + "//li[@role='option']";

    private static final String PANEL_ERRORS_XPATH = PANEL + "//p[contains(@class,'Mui-error')]";

    @FindBy(xpath = HEADING_XPATH)
    private WebElement heading;

    @FindBy(xpath = DESCRIPTION_XPATH)
    private WebElement description;

    @FindBy(xpath = ACCORDION_HEADER_XPATH)
    private WebElement accordionHeader;

    @FindBy(xpath = ACCORDION_TITLE_XPATH)
    private WebElement accordionTitle;

    @FindBy(xpath = ACCORDION_DESCRIPTION_XPATH)
    private WebElement accordionDescription;

    private final By accordionErrorIconLocator = By.xpath(ACCORDION_ERROR_ICON_XPATH);

    @FindBy(xpath = STATUS_INPUT_XPATH)
    private WebElement statusValueInput;

    @FindBy(xpath = STATUS_SELECT_XPATH)
    private WebElement statusSelect;
    private final By statusSelectLocator = By.xpath(STATUS_SELECT_XPATH);

    @FindBy(xpath = STATUS_LABEL_XPATH)
    private WebElement statusLabel;

    @FindBy(xpath = CHANNEL_INPUT_XPATH)
    private WebElement channelValueInput;

    @FindBy(xpath = CHANNEL_SELECT_XPATH)
    private WebElement channelSelect;
    private final By channelSelectLocator = By.xpath(CHANNEL_SELECT_XPATH);

    @FindBy(xpath = CHANNEL_LABEL_XPATH)
    private WebElement channelLabel;

    @FindBy(xpath = API_KEY_INPUT_XPATH)
    private WebElement apiKeyInput;
    private final By apiKeyInputLocator = By.xpath(API_KEY_INPUT_XPATH);

    @FindBy(xpath = API_KEY_LABEL_XPATH)
    private WebElement apiKeyLabel;

    @FindBy(xpath = API_KEY_MESSAGE_XPATH)
    private WebElement apiKeyMessage;

    @FindBy(xpath = API_KEY_ERROR_XPATH)
    private WebElement apiKeyError;
    private final By apiKeyErrorLocator = By.xpath(API_KEY_ERROR_XPATH);

    @FindBy(xpath = API_KEY_EYE_XPATH)
    private WebElement apiKeyEyeButton;
    private final By apiKeyEyeButtonLocator = By.xpath(API_KEY_EYE_XPATH);

    @FindBy(xpath = SENDER_ID_INPUT_XPATH)
    private WebElement senderIdInput;
    private final By senderIdInputLocator = By.xpath(SENDER_ID_INPUT_XPATH);

    @FindBy(xpath = SENDER_ID_LABEL_XPATH)
    private WebElement senderIdLabel;

    private final By senderIdErrorLocator = By.xpath(SENDER_ID_ERROR_XPATH);

    private final By listboxLocator = By.xpath(LISTBOX_XPATH);

    @FindBy(xpath = LISTBOX_XPATH)
    private WebElement listbox;

    @FindBy(xpath = OPTIONS_XPATH)
    private List<WebElement> options;

    @FindBy(xpath = PANEL_ERRORS_XPATH)
    private List<WebElement> panelErrors;

    // ---- locators that depend on an option ----

    /** A Status option by its machine value ("active" / "inactive"), which the portal exposes as data-value. */
    public static By statusOptionLocator(String status) {
        return By.xpath(LISTBOX_XPATH + "//li[@role='option'][@data-value=" + DashboardPageElements.xpathLiteral(status.toLowerCase()) + "]");
    }

    public static String statusOptionXpath(String status) {
        return LISTBOX_XPATH + "//li[@role='option'][@data-value=" + DashboardPageElements.xpathLiteral(status.toLowerCase()) + "]";
    }

    /** A Channel option by its visible text. */
    public static By channelOptionLocator(String channel) {
        return By.xpath(channelOptionXpath(channel));
    }

    public static String channelOptionXpath(String channel) {
        return OPTIONS_XPATH + "[normalize-space()=" + DashboardPageElements.xpathLiteral(channel) + "]";
    }
}
