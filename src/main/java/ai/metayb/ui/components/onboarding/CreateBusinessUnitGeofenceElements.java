package ai.metayb.ui.components.onboarding;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Locators for the Geofence section of the Create Business Unit dialog, from the live DOM.
 *
 * The section has two controls: Geofence Config Status (a MUI Select, found through its label) and Radius (a text input
 * with the stable name {@value #RADIUS_NAME}, an end adornment showing the unit "m", and a help paragraph that is
 * replaced by the validation message while the value is invalid; both are the same paragraph, which the input's
 * aria-describedby points at). Every Create Business Unit panel stays mounted (hidden), so everything is scoped to this
 * panel through its heading. The Radius label gains a trailing " *" while the status is Active. MUI renders the status
 * list outside the dialog, so its locators are document-level. Generated ids are never used.
 */
@Getter
public class CreateBusinessUnitGeofenceElements {

    public static final String RADIUS_NAME = "radius";

    private static final String DIALOG = DashboardPageElements.CREATE_DIALOG_XPATH;
    private static final String FORM_CONTROL = "/ancestor::div[contains(@class,'MuiFormControl-root')][1]";

    private static final String HEADING_XPATH = DIALOG + "//h6[normalize-space()='Geofence']";
    /** The panel: the box that holds the heading, the description and the two controls. */
    private static final String PANEL = HEADING_XPATH + "/parent::div";
    private static final String DESCRIPTION_XPATH =
            PANEL + "/p[normalize-space()='Set how close a user must be to a location before they can act on it.']";
    private static final String PANEL_ERRORS_XPATH = PANEL + "//p[contains(@class,'Mui-error')]";

    // ---- Geofence Config Status (MUI Select) ----
    private static final String STATUS_CONTROL = PANEL + "//label[normalize-space()='Geofence Config Status']" + FORM_CONTROL;
    private static final String STATUS_SELECT_XPATH = STATUS_CONTROL + "//div[@role='combobox']";
    private static final String STATUS_VALUE_INPUT_XPATH = STATUS_CONTROL + "//input[@aria-hidden='true']";

    // ---- Radius ----
    private static final String RADIUS_INPUT_XPATH = PANEL + "//input[@name='" + RADIUS_NAME + "']";
    private static final String RADIUS_LABEL_XPATH = RADIUS_INPUT_XPATH + FORM_CONTROL + "//label";
    private static final String RADIUS_UNIT_XPATH =
            RADIUS_INPUT_XPATH + "/ancestor::div[contains(@class,'MuiInputBase-root')][1]//p[normalize-space()='m']";
    /** The help text, or the error while the value is invalid: the same paragraph either way. */
    private static final String RADIUS_MESSAGE_XPATH =
            RADIUS_INPUT_XPATH + FORM_CONTROL + "//p[contains(@class,'MuiFormHelperText-root')]";
    private static final String RADIUS_ERROR_XPATH =
            RADIUS_INPUT_XPATH + FORM_CONTROL + "//p[contains(@class,'Mui-error')]";

    // ---- status list rendered by MUI outside the dialog ----
    private static final String LISTBOX_XPATH = "//ul[@role='listbox']";
    private static final String OPTIONS_XPATH = LISTBOX_XPATH + "//li[@role='option']";

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

    @FindBy(xpath = RADIUS_INPUT_XPATH)
    private WebElement radiusInput;

    @FindBy(xpath = RADIUS_LABEL_XPATH)
    private WebElement radiusLabel;

    @FindBy(xpath = RADIUS_UNIT_XPATH)
    private WebElement radiusUnit;

    @FindBy(xpath = RADIUS_MESSAGE_XPATH)
    private WebElement radiusMessage;

    @FindBy(xpath = RADIUS_ERROR_XPATH)
    private WebElement radiusError;
    private final By radiusErrorLocator = By.xpath(RADIUS_ERROR_XPATH);

    @FindBy(xpath = LISTBOX_XPATH)
    private WebElement listbox;
    private final By listboxLocator = By.xpath(LISTBOX_XPATH);

    @FindBy(xpath = OPTIONS_XPATH)
    private List<WebElement> options;

    // ---- locators that depend on an option ----

    /** A status option by its machine value ("active" / "inactive"), which the portal exposes as data-value. */
    public static By statusOptionLocator(String status) {
        return By.xpath(statusOptionXpath(status));
    }

    public static String statusOptionXpath(String status) {
        return LISTBOX_XPATH + "//li[@role='option'][@data-value=" + DashboardPageElements.xpathLiteral(status.toLowerCase()) + "]";
    }
}
