package ai.metayb.ui.pages.onboarding;

import ai.metayb.ui.components.onboarding.CreateBusinessUnitCdpElements;
import ai.metayb.ui.wrappers.BaseDriver;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The CDP Integration section of the Create Business Unit dialog. Use {@link CreateBusinessUnitDialog#selectSection}
 * to bring the section on screen first. Only unsaved form state is touched: there is no Create operation here.
 *
 * Validation in this section is edit-driven and only applies while CDP Config Status is Active: a required error
 * appears when the company name or the country is edited to empty, not on focus/blur and not when the status changes.
 * The company name is replaced with {@code replaceText} and the country is emptied with {@code clearField}, because
 * both are React-controlled. Neither field is sensitive.
 */
public class CreateBusinessUnitCdpSection extends BaseDriver {

    public static final String SECTION = "CDP Integration";
    public static final String ACTIVE = "Active";
    public static final String INACTIVE = "Inactive";
    /** A country the portal is known to offer; the full list and its order are deliberately not relied on. */
    public static final String GHANA = "Ghana";

    private final CreateBusinessUnitCdpElements elements;

    public CreateBusinessUnitCdpSection() {
        super();
        elements = new CreateBusinessUnitCdpElements();
        PageFactory.initElements(getDriver(), elements);
    }

    // ---------------------------------------------------------------- static content

    public String getCdpHeading() {
        return getText(elements.getHeading()).trim();
    }

    public String getCdpDescription() {
        return getText(elements.getDescription()).trim();
    }

    public boolean isCdpSectionDisplayed() {
        return isDisplayed(elements.getHeading());
    }

    /** Moves focus away from any field by clicking the section heading. */
    public void blurCdp() {
        click(elements.getHeading());
    }

    // ---------------------------------------------------------------- CDP Config Status (MUI Select)

    /** The text the select shows ("Active" / "Inactive"). */
    public String getConfigStatus() {
        return getText(elements.getStatusSelect()).trim();
    }

    /** The machine value held by the select's hidden input ("active" / "inactive"). */
    public String getConfigStatusValue() {
        String value = getAttribute(elements.getStatusValueInput(), "value");
        return value == null ? "" : value;
    }

    public void openConfigStatusList() {
        click(elements.getStatusSelect());
        waitForElementVisible(elements.getListboxLocator());
    }

    /** Opens the list with the ArrowDown key on the focused select. */
    public void openConfigStatusListWithKeyboard() {
        typeWithoutClear(elements.getStatusSelect(), Keys.ARROW_DOWN.toString());
        waitForElementVisible(elements.getListboxLocator());
    }

    /** Whether the status list is open. Its options carry data-value, which the country options do not. */
    public boolean isConfigStatusListOpen() {
        return isPresent(CreateBusinessUnitCdpElements.statusOptionLocator(ACTIVE));
    }

    /** Closes only the list with Escape (the value and the dialog stay). The key goes to the list that has focus. */
    public void closeConfigStatusListWithEscape() {
        typeWithoutClear(elements.getListbox(), Keys.ESCAPE.toString());
        waitForElementInvisible(elements.getListboxLocator());
    }

    /** The option texts, opening the list first if it is closed. The list is left open. */
    public List<String> getConfigStatusOptions() {
        if (!isConfigStatusListOpen()) {
            openConfigStatusList();
        }
        return optionTexts();
    }

    /** Whether this option is marked selected (aria-selected) in the open list. */
    public boolean isConfigStatusOptionSelected(String status) {
        waitForElementVisible(CreateBusinessUnitCdpElements.statusOptionLocator(status));
        WebElement option = locateElement("xpath", CreateBusinessUnitCdpElements.statusOptionXpath(status));
        return "true".equals(getAttribute(option, "aria-selected"));
    }

    /**
     * Opens the list if needed, clicks the option and waits for the list to close and the select to show it. A country
     * list left open (for example after emptying the country with the keyboard) is dismissed first.
     */
    public void selectConfigStatus(String status) {
        if (isCountryListOpen()) {
            closeCountryListWithEscape();
        }
        if (!isConfigStatusListOpen()) {
            openConfigStatusList();
        }
        waitForElementClickable(CreateBusinessUnitCdpElements.statusOptionLocator(status));
        click(locateElement("xpath", CreateBusinessUnitCdpElements.statusOptionXpath(status)));
        waitForElementInvisible(elements.getListboxLocator());
        if (!status.equals(getConfigStatus())) {
            reportStep("Expected CDP Config Status '" + status + "' but the select shows '" + getConfigStatus() + "'", "FAIL");
        }
    }

    // ---------------------------------------------------------------- CDP Company Name

    /** Replaces the company name (keyboard clear, then type), as the field is React-controlled. */
    public void enterCompanyName(String name) {
        replaceText(elements.getCompanyNameInput(), name);
    }

    /** Empties the company name with key presses so the form's own validation sees the edit. */
    public void clearCompanyName() {
        clearField(elements.getCompanyNameInput());
    }

    /** Focuses the company name without editing it. */
    public void focusCompanyName() {
        click(elements.getCompanyNameInput());
    }

    public String getCompanyName() {
        String value = getAttribute(elements.getCompanyNameInput(), "value");
        return value == null ? "" : value;
    }

    public boolean isCompanyNameEmpty() {
        return getCompanyName().isEmpty();
    }

    /** The label as displayed, including the " *" marker while the status is Active. */
    public String getCompanyNameLabel() {
        return getText(elements.getCompanyNameLabel()).trim();
    }

    public boolean isCompanyNameRequiredMarked() {
        return getCompanyNameLabel().contains("*");
    }

    public boolean isCompanyNameMarkedInvalid() {
        return "true".equals(getAttribute(elements.getCompanyNameInput(), "aria-invalid"));
    }

    /** Whether an error is currently shown for the company name. The error element is removed when it is valid. */
    public boolean isCompanyNameErrorDisplayed() {
        return isPresent(elements.getCompanyNameErrorLocator());
    }

    /** Waits for the company name's validation message, then returns its text. */
    public String getCompanyNameError() {
        waitForElementVisible(elements.getCompanyNameErrorLocator());
        return getText(elements.getCompanyNameError()).trim();
    }

    public void waitForCompanyNameErrorToDisappear() {
        waitForElementInvisible(elements.getCompanyNameErrorLocator());
    }

    /** True when aria-describedby is set and points at the company name's own error message. */
    public boolean isCompanyNameDescribedByItsError() {
        String describedBy = getAttribute(elements.getCompanyNameInput(), "aria-describedby");
        return describedBy != null && !describedBy.isBlank()
                && describedBy.equals(getAttribute(elements.getCompanyNameError(), "id"));
    }

    // ---------------------------------------------------------------- Country (Autocomplete)

    /** The text in the country box, e.g. "Ghana", or an empty string. */
    public String getCountry() {
        String value = getAttribute(elements.getCountryInput(), "value");
        return value == null ? "" : value;
    }

    /** Opens the country list with a click. Only call this when the list is closed: clicking an open, empty one closes it. */
    public void openCountryList() {
        click(elements.getCountryInput());
        waitForElementVisible(elements.getListboxLocator());
    }

    /**
     * Opens the country list with ArrowDown on the focused input. With an empty field the first press opens the list
     * without highlighting anything; with a value it opens the list on the selected option.
     */
    public void openCountryListWithKeyboard() {
        typeWithoutClear(elements.getCountryInput(), Keys.ARROW_DOWN.toString());
        waitForElementVisible(elements.getListboxLocator());
    }

    /** Whether the country list is open: a listbox is on screen and it is not the status list. */
    public boolean isCountryListOpen() {
        return isPresent(elements.getListboxLocator()) && !isConfigStatusListOpen();
    }

    /** Closes only the list with Escape; the typed text or value and the dialog stay. */
    public void closeCountryListWithEscape() {
        typeWithoutClear(elements.getCountryInput(), Keys.ESCAPE.toString());
        waitForElementInvisible(elements.getListboxLocator());
    }

    /**
     * Picks the option for this country by its visible text. The list may already be open (after reading its options or
     * after typing), and clicking an empty, open Autocomplete closes it, so it is only opened when closed.
     */
    public void selectCountry(String country) {
        if (!isCountryListOpen()) {
            openCountryList();
        }
        waitForElementClickable(CreateBusinessUnitCdpElements.countryOptionLocator(country));
        click(locateElement("xpath", CreateBusinessUnitCdpElements.countryOptionXpath(country)));
        waitForElementInvisible(elements.getListboxLocator());
        if (!country.equals(getCountry())) {
            reportStep("Expected Country '" + country + "' but the field shows '" + getCountry() + "'", "FAIL");
        }
    }

    /**
     * Picks the highlighted option with the keyboard: ArrowDown highlights one and Enter selects it. The list must be
     * open (see {@link #openCountryListWithKeyboard}), since the press that opens an empty field highlights nothing.
     */
    public void selectHighlightedCountryWithKeyboard() {
        typeWithoutClear(elements.getCountryInput(), Keys.ARROW_DOWN.toString());
        typeWithoutClear(elements.getCountryInput(), Keys.ENTER.toString());
        waitForElementInvisible(elements.getListboxLocator());
    }

    /** Replaces the text in the search box (the list opens as you type). Does not select anything. */
    public void searchCountry(String text) {
        replaceText(elements.getCountryInput(), text);
    }

    /** Empties the country with key presses, which removes the selection the way a user would. */
    public void clearCountryWithKeyboard() {
        clearField(elements.getCountryInput());
    }

    /**
     * Empties the country with its Clear (X) button, which MUI shows while the box has a value and focus or hover, so
     * call this right after selecting. The list stays closed afterwards.
     */
    public void clearCountryWithButton() {
        waitForElementClickable(elements.getCountryClearButtonLocator());
        click(elements.getCountryClearButton());
        if (!getCountry().isEmpty()) {
            reportStep("Expected the Clear button to empty the country but it shows '" + getCountry() + "'", "FAIL");
        }
    }

    /** The option texts of the open country list. Waits for the list. */
    public List<String> getCountryOptions() {
        waitForElementVisible(elements.getListboxLocator());
        return optionTexts();
    }

    public boolean isCountryOptionListed(String country) {
        return isPresent(CreateBusinessUnitCdpElements.countryOptionLocator(country));
    }

    public void waitForNoCountryOptions() {
        waitForElementVisible(elements.getNoOptionsLocator());
    }

    public boolean hasNoCountryOptions() {
        return isDisplayed(elements.getNoOptions());
    }

    public String getNoCountryOptionsText() {
        return getText(elements.getNoOptions()).trim();
    }

    public boolean isCountryErrorDisplayed() {
        return isPresent(elements.getCountryErrorLocator());
    }

    /** Waits for the country's validation message, then returns its text. */
    public String getCountryError() {
        waitForElementVisible(elements.getCountryErrorLocator());
        return getText(elements.getCountryError()).trim();
    }

    public void waitForCountryErrorToDisappear() {
        waitForElementInvisible(elements.getCountryErrorLocator());
    }

    public boolean isCountryMarkedInvalid() {
        return "true".equals(getAttribute(elements.getCountryInput(), "aria-invalid"));
    }

    public boolean isCountryRequiredMarked() {
        return getText(elements.getCountryLabel()).contains("*");
    }

    /** True when the country's aria-describedby is set and points at its own error message. */
    public boolean isCountryDescribedByItsError() {
        String describedBy = getAttribute(elements.getCountryInput(), "aria-describedby");
        return describedBy != null && !describedBy.isBlank()
                && describedBy.equals(getAttribute(elements.getCountryError(), "id"));
    }

    // ---------------------------------------------------------------- whole section

    /** The validation messages currently shown anywhere in the CDP section. */
    public List<String> getVisibleErrors() {
        List<String> messages = new ArrayList<>();
        for (WebElement error : elements.getPanelErrors()) {
            messages.add(getText(error).trim());
        }
        return messages;
    }

    // ----------------------------------------------------------------

    private List<String> optionTexts() {
        List<String> texts = new ArrayList<>();
        for (WebElement option : elements.getOptions()) {
            texts.add(getText(option).trim());
        }
        return texts;
    }
}
