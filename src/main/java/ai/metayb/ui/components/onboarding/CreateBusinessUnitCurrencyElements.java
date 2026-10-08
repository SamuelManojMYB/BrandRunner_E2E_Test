package ai.metayb.ui.components.onboarding;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Locators for the Currency section of the Create Business Unit dialog, from the live DOM. The field is a MUI
 * Autocomplete (single-select, searchable). The dialog holds several comboboxes, so the field is always found
 * through its label; generated ids are never used. The option list is rendered by MUI in a popper outside the
 * dialog, so the listbox, options and "No options" locators are document-level.
 */
@Getter
public class CreateBusinessUnitCurrencyElements {

    private static final String DIALOG = DashboardPageElements.CREATE_DIALOG_XPATH;
    private static final String LABEL_XPATH = DIALOG + "//label[normalize-space()='Currency *']";
    private static final String AUTOCOMPLETE = LABEL_XPATH + "/ancestor::div[contains(@class,'MuiAutocomplete-root')][1]";

    private static final String HEADING_XPATH = DIALOG + "//h6[normalize-space()='Currency']";
    private static final String DESCRIPTION_XPATH =
            DIALOG + "//p[normalize-space()='Set the default currency for this business unit.']";
    private static final String COMBOBOX_XPATH = AUTOCOMPLETE + "//input[@role='combobox']";
    // The popup button's aria-label flips between "Open" and "Close", so it is found by MUI's static class.
    private static final String POPUP_BUTTON_XPATH = AUTOCOMPLETE + "//button[contains(@class,'MuiAutocomplete-popupIndicator')]";
    private static final String CLEAR_BUTTON_XPATH = AUTOCOMPLETE + "//button[@aria-label='Clear']";
    private static final String ERROR_XPATH = LABEL_XPATH
            + "/ancestor::div[contains(@class,'MuiFormControl-root')][1]//p[contains(@class,'Mui-error')]";

    private static final String LISTBOX_XPATH = "//ul[@role='listbox']";
    private static final String OPTIONS_XPATH = LISTBOX_XPATH + "//li[@role='option']";
    private static final String NO_OPTIONS_XPATH = "//div[contains(@class,'MuiAutocomplete-noOptions')]";

    @FindBy(xpath = HEADING_XPATH)
    private WebElement heading;

    @FindBy(xpath = DESCRIPTION_XPATH)
    private WebElement description;

    @FindBy(xpath = LABEL_XPATH)
    private WebElement label;

    @FindBy(xpath = COMBOBOX_XPATH)
    private WebElement combobox;
    private final By comboboxLocator = By.xpath(COMBOBOX_XPATH);

    @FindBy(xpath = POPUP_BUTTON_XPATH)
    private WebElement popupButton;

    @FindBy(xpath = CLEAR_BUTTON_XPATH)
    private WebElement clearButton;
    private final By clearButtonLocator = By.xpath(CLEAR_BUTTON_XPATH);

    @FindBy(xpath = ERROR_XPATH)
    private WebElement error;
    private final By errorLocator = By.xpath(ERROR_XPATH);

    private final By listboxLocator = By.xpath(LISTBOX_XPATH);

    @FindBy(xpath = OPTIONS_XPATH)
    private List<WebElement> options;

    @FindBy(xpath = NO_OPTIONS_XPATH)
    private WebElement noOptions;
    private final By noOptionsLocator = By.xpath(NO_OPTIONS_XPATH);

    // ---- locators that depend on a currency code (options read "INR (₹)": match the code, never the symbol) ----

    public static String optionXpath(String currencyCode) {
        return OPTIONS_XPATH + "[starts-with(normalize-space(),"
                + DashboardPageElements.xpathLiteral(currencyCode + " (") + ")]";
    }

    public static By optionLocator(String currencyCode) {
        return By.xpath(optionXpath(currencyCode));
    }
}
