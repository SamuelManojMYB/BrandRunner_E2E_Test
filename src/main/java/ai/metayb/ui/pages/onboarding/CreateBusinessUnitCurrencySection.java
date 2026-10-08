package ai.metayb.ui.pages.onboarding;

import ai.metayb.ui.components.onboarding.CreateBusinessUnitCurrencyElements;
import ai.metayb.ui.wrappers.BaseDriver;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The Currency section of the Create Business Unit dialog. Use {@link CreateBusinessUnitDialog#selectSection}
 * to bring the section on screen first; everything here works on what is displayed. Only unsaved form state is
 * touched: there is no Create operation anywhere in these classes.
 *
 * Selection is by currency code ("INR"), never by symbol: "$" and "¥" are shared by several options.
 */
public class CreateBusinessUnitCurrencySection extends BaseDriver {

    public static final String SECTION = "Currency";

    private final CreateBusinessUnitCurrencyElements elements;

    public CreateBusinessUnitCurrencySection() {
        super();
        elements = new CreateBusinessUnitCurrencyElements();
        PageFactory.initElements(getDriver(), elements);
    }

    // ---------------------------------------------------------------- static content

    public String getCurrencyHeading() {
        return getText(elements.getHeading()).trim();
    }

    public String getCurrencyDescription() {
        return getText(elements.getDescription()).trim();
    }

    /** The label text including the required marker, e.g. "Currency *". */
    public String getCurrencyLabel() {
        return getText(elements.getLabel()).trim();
    }

    public boolean isCurrencyFieldDisplayed() {
        return isDisplayed(elements.getCombobox());
    }

    // ---------------------------------------------------------------- value

    /** The text in the field, e.g. "INR (₹)", or an empty string when nothing is selected. */
    public String getSelectedCurrency() {
        String value = getAttribute(elements.getCombobox(), "value");
        return value == null ? "" : value;
    }

    /** Opens the list, clicks the option for this code and waits for the list to close and the value to show it. */
    public void selectCurrency(String currencyCode) {
        selectCustomDropdownOption(elements.getCombobox(), elements.getListboxLocator(),
                CreateBusinessUnitCurrencyElements.optionLocator(currencyCode));
        waitForElementInvisible(elements.getListboxLocator());
        if (!getSelectedCurrency().startsWith(currencyCode)) {
            reportStep("Expected the currency " + currencyCode + " to be selected but the field shows '"
                    + getSelectedCurrency() + "'", "FAIL");
        }
    }

    /**
     * Keyboard selection: type the code to filter, move to the first match and press Enter. The wrapper has no
     * key-press method, so the keys are sent as text through typeWithoutClear.
     */
    public void selectCurrencyWithKeyboard(String currencyCode) {
        typeWithoutClear(elements.getCombobox(), currencyCode.toLowerCase());
        waitForElementVisible(CreateBusinessUnitCurrencyElements.optionLocator(currencyCode));
        typeWithoutClear(elements.getCombobox(), Keys.ARROW_DOWN.toString());
        typeWithoutClear(elements.getCombobox(), Keys.ENTER.toString());
        waitForElementInvisible(elements.getListboxLocator());
        if (!getSelectedCurrency().startsWith(currencyCode)) {
            reportStep("Expected the currency " + currencyCode + " to be selected but the field shows '"
                    + getSelectedCurrency() + "'", "FAIL");
        }
    }

    /** Replaces the text in the search box (the list opens as you type). Does not select anything. */
    public void searchCurrency(String text) {
        replaceText(elements.getCombobox(), text);
    }

    /** Clicks the Clear button, which is shown while the field holds a value and has focus. */
    public void clearCurrency() {
        waitForElementClickable(elements.getClearButtonLocator());
        click(elements.getClearButton());
    }

    /** Deletes the value with Backspace key presses, which empties the field the way a user would. */
    public void clearCurrencyByKeyboard() {
        clearField(elements.getCombobox());
    }

    // ---------------------------------------------------------------- list

    /** Clicks the field, which opens the list, and waits for it. */
    public void openCurrencyList() {
        click(elements.getCombobox());
        waitForElementVisible(elements.getListboxLocator());
    }

    public boolean isCurrencyListOpen() {
        return isPresent(elements.getListboxLocator());
    }

    /** Closes only the list with Escape (the selected value and the dialog stay). */
    public void closeCurrencyListWithEscape() {
        typeWithoutClear(elements.getCombobox(), Keys.ESCAPE.toString());
        waitForElementInvisible(elements.getListboxLocator());
    }

    /** The option texts currently listed ("INR (₹)", ...). Waits for the list, so it must be open or opening. */
    public List<String> getCurrencyOptions() {
        waitForElementVisible(elements.getListboxLocator());
        List<String> texts = new ArrayList<>();
        for (WebElement option : elements.getOptions()) {
            texts.add(getText(option).trim());
        }
        return texts;
    }

    public boolean isCurrencyOptionListed(String currencyCode) {
        return isPresent(CreateBusinessUnitCurrencyElements.optionLocator(currencyCode));
    }

    /** Whether the option for this code is marked selected (aria-selected) in the open list. */
    public boolean isCurrencyOptionSelected(String currencyCode) {
        waitForElementVisible(CreateBusinessUnitCurrencyElements.optionLocator(currencyCode));
        WebElement option = locateElement("xpath", CreateBusinessUnitCurrencyElements.optionXpath(currencyCode));
        return "true".equals(getAttribute(option, "aria-selected"));
    }

    public void waitForNoCurrencyOptions() {
        waitForElementVisible(elements.getNoOptionsLocator());
    }

    public boolean hasNoCurrencyOptions() {
        return isDisplayed(elements.getNoOptions());
    }

    public String getNoCurrencyOptionsText() {
        return getText(elements.getNoOptions()).trim();
    }

    // ---------------------------------------------------------------- focus and validation

    /** Moves focus away by clicking the section heading. */
    public void blurCurrency() {
        click(elements.getHeading());
    }

    public boolean isCurrencyErrorDisplayed() {
        return isDisplayed(elements.getError());
    }

    /** Waits for the validation message to appear, then returns its text. */
    public String getCurrencyError() {
        waitForElementVisible(elements.getErrorLocator());
        return getText(elements.getError()).trim();
    }

    public void waitForCurrencyErrorToDisappear() {
        waitForElementInvisible(elements.getErrorLocator());
    }

    public boolean isCurrencyMarkedInvalid() {
        return "true".equals(getAttribute(elements.getCombobox(), "aria-invalid"));
    }
}
