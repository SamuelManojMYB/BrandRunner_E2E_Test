package ai.metayb.ui.components.onboarding;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Locators for the Payment Integration section of the Create Business Unit dialog, from the live DOM.
 *
 * The section has three controls. The secret key is the only one with a stable {@code name}
 * ({@value #SECRET_KEY_NAME}). Payment Config Status is a MUI Select and Payment Provider is a MUI Autocomplete; both
 * are found through their labels, and the labels of the required ones gain a trailing " *" while the status is
 * Active, so the provider label is matched with starts-with. The portal gives the secret key's show/hide button no
 * aria-label or title, so it is located structurally and kept inside the secret input's own input component. MUI
 * renders both option lists outside the dialog, so their locators are document-level. Generated ids are never used.
 */
@Getter
public class CreateBusinessUnitPaymentElements {

    public static final String SECRET_KEY_NAME = "PAYSTACK_SECRET_KEY";

    private static final String DIALOG = DashboardPageElements.CREATE_DIALOG_XPATH;
    private static final String FORM_CONTROL = "/ancestor::div[contains(@class,'MuiFormControl-root')][1]";

    private static final String HEADING_XPATH = DIALOG + "//h6[normalize-space()='Payment Integration']";
    private static final String DESCRIPTION_XPATH =
            DIALOG + "//p[normalize-space()='Configure the payment provider connection for this business unit.']";
    private static final String PANEL_ERRORS_XPATH = HEADING_XPATH + "/parent::div//p[contains(@class,'Mui-error')]";

    // ---- Payment Config Status (MUI Select) ----
    private static final String STATUS_CONTROL =
            DIALOG + "//label[normalize-space()='Payment Config Status']" + FORM_CONTROL;
    private static final String STATUS_SELECT_XPATH = STATUS_CONTROL + "//div[@role='combobox']";
    private static final String STATUS_VALUE_INPUT_XPATH = STATUS_CONTROL + "//input[@aria-hidden='true']";

    // ---- Payment Provider (Autocomplete) ----
    private static final String PROVIDER_LABEL = DIALOG + "//label[starts-with(normalize-space(),'Payment Provider')]";
    private static final String PROVIDER_ROOT = PROVIDER_LABEL + "/ancestor::div[contains(@class,'MuiAutocomplete-root')][1]";
    private static final String PROVIDER_INPUT_XPATH = PROVIDER_ROOT + "//input[@role='combobox']";
    private static final String PROVIDER_CLEAR_XPATH = PROVIDER_ROOT + "//button[@aria-label='Clear']";
    private static final String PROVIDER_ERROR_XPATH = PROVIDER_LABEL + FORM_CONTROL + "//p[contains(@class,'Mui-error')]";

    // ---- PAYSTACK Secret Key ----
    private static final String SECRET_INPUT_XPATH = DIALOG + "//input[@name='" + SECRET_KEY_NAME + "']";
    private static final String SECRET_LABEL_XPATH = SECRET_INPUT_XPATH + FORM_CONTROL + "//label";
    private static final String SECRET_ERROR_XPATH = SECRET_INPUT_XPATH + FORM_CONTROL + "//p[contains(@class,'Mui-error')]";
    /** The show/hide button lives in the secret input's own MuiInputBase component, so no other button can match. */
    private static final String SECRET_EYE_XPATH =
            SECRET_INPUT_XPATH + "/ancestor::div[contains(@class,'MuiInputBase-root')][1]//button";

    // ---- option lists rendered by MUI outside the dialog (only one is open at a time) ----
    private static final String LISTBOX_XPATH = "//ul[@role='listbox']";
    private static final String OPTIONS_XPATH = LISTBOX_XPATH + "//li[@role='option']";
    private static final String NO_OPTIONS_XPATH = "//div[contains(@class,'MuiAutocomplete-noOptions')]";

    @FindBy(xpath = HEADING_XPATH)
    private WebElement heading;

    @FindBy(xpath = DESCRIPTION_XPATH)
    private WebElement description;

    @FindBy(xpath = PANEL_ERRORS_XPATH)
    private List<WebElement> panelErrors;

    @FindBy(xpath = STATUS_SELECT_XPATH)
    private WebElement statusSelect;

    @FindBy(xpath = STATUS_VALUE_INPUT_XPATH)
    private WebElement statusValueInput;

    @FindBy(xpath = PROVIDER_LABEL)
    private WebElement providerLabel;

    @FindBy(xpath = PROVIDER_INPUT_XPATH)
    private WebElement providerInput;

    @FindBy(xpath = PROVIDER_CLEAR_XPATH)
    private WebElement providerClearButton;
    private final By providerClearButtonLocator = By.xpath(PROVIDER_CLEAR_XPATH);

    @FindBy(xpath = PROVIDER_ERROR_XPATH)
    private WebElement providerError;
    private final By providerErrorLocator = By.xpath(PROVIDER_ERROR_XPATH);

    @FindBy(xpath = SECRET_INPUT_XPATH)
    private WebElement secretKeyInput;

    @FindBy(xpath = SECRET_LABEL_XPATH)
    private WebElement secretKeyLabel;

    @FindBy(xpath = SECRET_ERROR_XPATH)
    private WebElement secretKeyError;
    private final By secretKeyErrorLocator = By.xpath(SECRET_ERROR_XPATH);

    @FindBy(xpath = SECRET_EYE_XPATH)
    private WebElement secretKeyEyeButton;
    private final By secretKeyEyeButtonLocator = By.xpath(SECRET_EYE_XPATH);

    @FindBy(xpath = LISTBOX_XPATH)
    private WebElement listbox;
    private final By listboxLocator = By.xpath(LISTBOX_XPATH);

    @FindBy(xpath = OPTIONS_XPATH)
    private List<WebElement> options;

    @FindBy(xpath = NO_OPTIONS_XPATH)
    private WebElement noOptions;
    private final By noOptionsLocator = By.xpath(NO_OPTIONS_XPATH);

    // ---- locators that depend on an option ----

    /** A status option by its machine value ("active" / "inactive"), which the portal exposes as data-value. */
    public static By statusOptionLocator(String status) {
        return By.xpath(statusOptionXpath(status));
    }

    public static String statusOptionXpath(String status) {
        return LISTBOX_XPATH + "//li[@role='option'][@data-value=" + DashboardPageElements.xpathLiteral(status.toLowerCase()) + "]";
    }

    /** A provider option by its visible text. */
    public static By providerOptionLocator(String provider) {
        return By.xpath(providerOptionXpath(provider));
    }

    public static String providerOptionXpath(String provider) {
        return OPTIONS_XPATH + "[normalize-space()=" + DashboardPageElements.xpathLiteral(provider) + "]";
    }
}
