package ai.metayb.ui.components.onboarding;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Locators for the CDP Integration section of the Create Business Unit dialog, from the live DOM.
 *
 * The section has three controls. The company name is the only one with a stable {@code name}
 * ({@value #COMPANY_NAME_NAME}). CDP Config Status is a MUI Select and Country is a MUI Autocomplete; both are found
 * through their labels. Every Create Business Unit panel stays mounted (hidden) while another is selected, so every
 * locator that is not a name is scoped to this panel through its heading, never to the whole dialog: an unscoped
 * {@code input[@role='combobox']} would resolve to another panel's field. The labels of the required controls gain a
 * trailing " *" while the status is Active, so the Country label is matched with starts-with. MUI renders both option
 * lists outside the dialog, so their locators are document-level. Generated ids are never used.
 */
@Getter
public class CreateBusinessUnitCdpElements {

    public static final String COMPANY_NAME_NAME = "CDP_COMPANY_NAME";

    private static final String DIALOG = DashboardPageElements.CREATE_DIALOG_XPATH;
    private static final String FORM_CONTROL = "/ancestor::div[contains(@class,'MuiFormControl-root')][1]";

    private static final String HEADING_XPATH = DIALOG + "//h6[normalize-space()='CDP Integration']";
    /** The panel: the box that holds the heading, the description and the three controls. */
    private static final String PANEL = HEADING_XPATH + "/parent::div";
    private static final String DESCRIPTION_XPATH =
            PANEL + "/p[normalize-space()='Configure the CDP connection for this business unit.']";
    private static final String PANEL_ERRORS_XPATH = PANEL + "//p[contains(@class,'Mui-error')]";

    // ---- CDP Config Status (MUI Select) ----
    private static final String STATUS_CONTROL = PANEL + "//label[normalize-space()='CDP Config Status']" + FORM_CONTROL;
    private static final String STATUS_SELECT_XPATH = STATUS_CONTROL + "//div[@role='combobox']";
    private static final String STATUS_VALUE_INPUT_XPATH = STATUS_CONTROL + "//input[@aria-hidden='true']";

    // ---- CDP Company Name ----
    private static final String COMPANY_NAME_INPUT_XPATH = PANEL + "//input[@name='" + COMPANY_NAME_NAME + "']";
    private static final String COMPANY_NAME_LABEL_XPATH = COMPANY_NAME_INPUT_XPATH + FORM_CONTROL + "//label";
    private static final String COMPANY_NAME_ERROR_XPATH =
            COMPANY_NAME_INPUT_XPATH + FORM_CONTROL + "//p[contains(@class,'Mui-error')]";

    // ---- Country (Autocomplete) ----
    private static final String COUNTRY_LABEL = PANEL + "//label[starts-with(normalize-space(),'Country')]";
    private static final String COUNTRY_ROOT = COUNTRY_LABEL + "/ancestor::div[contains(@class,'MuiAutocomplete-root')][1]";
    private static final String COUNTRY_INPUT_XPATH = COUNTRY_ROOT + "//input[@role='combobox']";
    private static final String COUNTRY_CLEAR_XPATH = COUNTRY_ROOT + "//button[@aria-label='Clear']";
    private static final String COUNTRY_ERROR_XPATH = COUNTRY_LABEL + FORM_CONTROL + "//p[contains(@class,'Mui-error')]";

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

    @FindBy(xpath = COMPANY_NAME_INPUT_XPATH)
    private WebElement companyNameInput;

    @FindBy(xpath = COMPANY_NAME_LABEL_XPATH)
    private WebElement companyNameLabel;

    @FindBy(xpath = COMPANY_NAME_ERROR_XPATH)
    private WebElement companyNameError;
    private final By companyNameErrorLocator = By.xpath(COMPANY_NAME_ERROR_XPATH);

    @FindBy(xpath = COUNTRY_LABEL)
    private WebElement countryLabel;

    @FindBy(xpath = COUNTRY_INPUT_XPATH)
    private WebElement countryInput;

    @FindBy(xpath = COUNTRY_CLEAR_XPATH)
    private WebElement countryClearButton;
    private final By countryClearButtonLocator = By.xpath(COUNTRY_CLEAR_XPATH);

    @FindBy(xpath = COUNTRY_ERROR_XPATH)
    private WebElement countryError;
    private final By countryErrorLocator = By.xpath(COUNTRY_ERROR_XPATH);

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

    /** A country option by its visible text. */
    public static By countryOptionLocator(String country) {
        return By.xpath(countryOptionXpath(country));
    }

    public static String countryOptionXpath(String country) {
        return OPTIONS_XPATH + "[normalize-space()=" + DashboardPageElements.xpathLiteral(country) + "]";
    }
}
