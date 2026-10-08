package ai.metayb.ui.components.onboarding;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Locators for the ERP Integration section of the Create Business Unit dialog, from the live DOM.
 *
 * The nine text inputs are found by their {@code name} attribute (see {@link ErpField}). The two selection
 * controls have no name, so they are found through their label: ERP Config Status is a MUI Select (a
 * {@code div[role=combobox]} with a hidden input holding the machine value) and ERP Provider is an Autocomplete.
 * Labels gain a trailing " *" while the status is Active, so label matching uses starts-with where it matters.
 * MUI renders both option lists outside the dialog, so their locators are document-level. Generated ids are
 * never used.
 */
@Getter
public class CreateBusinessUnitErpElements {

    private static final String DIALOG = DashboardPageElements.CREATE_DIALOG_XPATH;
    private static final String FORM_CONTROL = "/ancestor::div[contains(@class,'MuiFormControl-root')][1]";

    private static final String HEADING_XPATH = DIALOG + "//h6[normalize-space()='ERP Integration']";
    private static final String DESCRIPTION_XPATH =
            DIALOG + "//p[normalize-space()='Configure the ERP system connection for this business unit.']";
    private static final String PANEL_ERRORS_XPATH = HEADING_XPATH + "/parent::div//p[contains(@class,'Mui-error')]";

    // ---- ERP Config Status (MUI Select) ----
    private static final String STATUS_CONTROL =
            DIALOG + "//label[normalize-space()='ERP Config Status']" + FORM_CONTROL;
    private static final String STATUS_SELECT_XPATH = STATUS_CONTROL + "//div[@role='combobox']";
    private static final String STATUS_VALUE_INPUT_XPATH = STATUS_CONTROL + "//input[@aria-hidden='true']";

    // ---- ERP Provider (Autocomplete) ----
    private static final String PROVIDER_LABEL = DIALOG + "//label[starts-with(normalize-space(),'ERP Provider')]";
    private static final String PROVIDER_ROOT = PROVIDER_LABEL + "/ancestor::div[contains(@class,'MuiAutocomplete-root')][1]";
    private static final String PROVIDER_INPUT_XPATH = PROVIDER_ROOT + "//input[@role='combobox']";
    private static final String PROVIDER_ERROR_XPATH = PROVIDER_LABEL + FORM_CONTROL + "//p[contains(@class,'Mui-error')]";

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

    @FindBy(xpath = PROVIDER_ERROR_XPATH)
    private WebElement providerError;
    private final By providerErrorLocator = By.xpath(PROVIDER_ERROR_XPATH);

    @FindBy(xpath = LISTBOX_XPATH)
    private WebElement listbox;
    private final By listboxLocator = By.xpath(LISTBOX_XPATH);

    @FindBy(xpath = OPTIONS_XPATH)
    private List<WebElement> options;

    @FindBy(xpath = NO_OPTIONS_XPATH)
    private WebElement noOptions;
    private final By noOptionsLocator = By.xpath(NO_OPTIONS_XPATH);

    // ---- locators that depend on a field or option ----

    public static String inputXpath(ErpField field) {
        return DIALOG + "//input[@name='" + field.inputName() + "']";
    }

    public static String labelXpath(ErpField field) {
        return inputXpath(field) + FORM_CONTROL + "//label";
    }

    public static String errorXpath(ErpField field) {
        return inputXpath(field) + FORM_CONTROL + "//p[contains(@class,'Mui-error')]";
    }

    public static By errorLocator(ErpField field) {
        return By.xpath(errorXpath(field));
    }

    /** A status option by its machine value ("active" / "inactive"), which the portal exposes as data-value. */
    public static By statusOptionLocator(String status) {
        return By.xpath(LISTBOX_XPATH + "//li[@role='option'][@data-value="
                + DashboardPageElements.xpathLiteral(status.toLowerCase()) + "]");
    }

    public static String statusOptionXpath(String status) {
        return LISTBOX_XPATH + "//li[@role='option'][@data-value=" + DashboardPageElements.xpathLiteral(status.toLowerCase()) + "]";
    }

    public static By providerOptionLocator(String provider) {
        return By.xpath(providerOptionXpath(provider));
    }

    public static String providerOptionXpath(String provider) {
        return OPTIONS_XPATH + "[normalize-space()=" + DashboardPageElements.xpathLiteral(provider) + "]";
    }
}
