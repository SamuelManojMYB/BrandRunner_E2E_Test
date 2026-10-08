package ai.metayb.ui.pages.onboarding;

import ai.metayb.ui.components.onboarding.CreateBusinessUnitErpElements;
import ai.metayb.ui.components.onboarding.ErpField;
import ai.metayb.ui.wrappers.BaseDriver;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The ERP Integration section of the Create Business Unit dialog. Use {@link CreateBusinessUnitDialog#selectSection}
 * to bring the section on screen first. Only unsaved form state is touched: there is no Create operation here.
 *
 * Validation in this section is edit-driven and only applies while ERP Config Status is Active: a required error
 * appears when a field is edited to empty, not on focus/blur and not when the status changes. Text values are
 * replaced with {@code replaceText} (keyboard clear, then type) because these inputs are React-controlled.
 * The MS Client Secret value goes through {@code typeSecret} and is never read back or logged.
 */
public class CreateBusinessUnitErpSection extends BaseDriver {

    public static final String SECTION = "ERP Integration";
    public static final String ACTIVE = "Active";
    public static final String INACTIVE = "Inactive";

    private final CreateBusinessUnitErpElements elements;

    public CreateBusinessUnitErpSection() {
        super();
        elements = new CreateBusinessUnitErpElements();
        PageFactory.initElements(getDriver(), elements);
    }

    // ---------------------------------------------------------------- static content

    public String getErpHeading() {
        return getText(elements.getHeading()).trim();
    }

    public String getErpDescription() {
        return getText(elements.getDescription()).trim();
    }

    public boolean isErpSectionDisplayed() {
        return isDisplayed(elements.getHeading());
    }

    /** Moves focus away from any field by clicking the section heading. */
    public void blurErp() {
        click(elements.getHeading());
    }

    // ---------------------------------------------------------------- ERP Config Status (MUI Select)

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

    public boolean isConfigStatusListOpen() {
        return isPresent(elements.getListboxLocator());
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
        waitForElementVisible(CreateBusinessUnitErpElements.statusOptionLocator(status));
        WebElement option = locateElement("xpath", CreateBusinessUnitErpElements.statusOptionXpath(status));
        return "true".equals(getAttribute(option, "aria-selected"));
    }

    /** Opens the list, clicks the option and waits for the list to close and the select to show it. */
    public void selectConfigStatus(String status) {
        if (!isConfigStatusListOpen()) {
            openConfigStatusList();
        }
        waitForElementClickable(CreateBusinessUnitErpElements.statusOptionLocator(status));
        click(locateElement("xpath", CreateBusinessUnitErpElements.statusOptionXpath(status)));
        waitForElementInvisible(elements.getListboxLocator());
        if (!status.equals(getConfigStatus())) {
            reportStep("Expected ERP Config Status '" + status + "' but the select shows '" + getConfigStatus() + "'", "FAIL");
        }
    }

    // ---------------------------------------------------------------- ERP Provider (Autocomplete)

    /** The text in the provider box, e.g. "Fabric", or an empty string. */
    public String getProvider() {
        String value = getAttribute(elements.getProviderInput(), "value");
        return value == null ? "" : value;
    }

    public void openProviderList() {
        click(elements.getProviderInput());
        waitForElementVisible(elements.getListboxLocator());
    }

    public boolean isProviderListOpen() {
        return isPresent(elements.getListboxLocator());
    }

    public void closeProviderListWithEscape() {
        typeWithoutClear(elements.getProviderInput(), Keys.ESCAPE.toString());
        waitForElementInvisible(elements.getListboxLocator());
    }

    /**
     * Picks the option for this provider. The list may already be open (after reading its options, or after typing or
     * clearing with the keyboard), and clicking an empty, open Autocomplete closes it, so it is only opened when closed.
     */
    public void selectProvider(String provider) {
        if (!isProviderListOpen()) {
            openProviderList();
        }
        waitForElementClickable(CreateBusinessUnitErpElements.providerOptionLocator(provider));
        click(locateElement("xpath", CreateBusinessUnitErpElements.providerOptionXpath(provider)));
        waitForElementInvisible(elements.getListboxLocator());
        if (!provider.equals(getProvider())) {
            reportStep("Expected ERP Provider '" + provider + "' but the field shows '" + getProvider() + "'", "FAIL");
        }
    }

    /** Replaces the text in the search box (the list opens as you type). Does not select anything. */
    public void searchProvider(String text) {
        replaceText(elements.getProviderInput(), text);
    }

    /** Empties the provider with key presses, which removes the selection the way a user would. */
    public void clearProvider() {
        clearField(elements.getProviderInput());
    }

    /** The option texts of the open provider list. Waits for the list. */
    public List<String> getProviderOptions() {
        waitForElementVisible(elements.getListboxLocator());
        return optionTexts();
    }

    public boolean isProviderOptionListed(String provider) {
        return isPresent(CreateBusinessUnitErpElements.providerOptionLocator(provider));
    }

    public void waitForNoProviderOptions() {
        waitForElementVisible(elements.getNoOptionsLocator());
    }

    public boolean hasNoProviderOptions() {
        return isDisplayed(elements.getNoOptions());
    }

    public String getNoProviderOptionsText() {
        return getText(elements.getNoOptions()).trim();
    }

    public boolean isProviderErrorDisplayed() {
        return isDisplayed(elements.getProviderError());
    }

    /** Waits for the provider's validation message, then returns its text. */
    public String getProviderError() {
        waitForElementVisible(elements.getProviderErrorLocator());
        return getText(elements.getProviderError()).trim();
    }

    public void waitForProviderErrorToDisappear() {
        waitForElementInvisible(elements.getProviderErrorLocator());
    }

    public boolean isProviderMarkedInvalid() {
        return "true".equals(getAttribute(elements.getProviderInput(), "aria-invalid"));
    }

    public boolean isProviderRequiredMarked() {
        return getText(elements.getProviderLabel()).contains("*");
    }

    // ---------------------------------------------------------------- text fields (by name attribute)

    public boolean isFieldDisplayed(ErpField field) {
        return isDisplayed(input(field));
    }

    /**
     * Replaces the field's value. The secret field is cleared by key presses and then typed with typeSecret, so its
     * value never reaches the log or the report.
     */
    public void enterFieldValue(ErpField field, String value) {
        WebElement input = input(field);
        if (field.isSecret()) {
            clearField(input);
            typeSecret(input, value);
        } else {
            replaceText(input, value);
        }
    }

    /** Empties the field with key presses so the form's own validation sees the edit. */
    public void clearFieldValue(ErpField field) {
        clearField(input(field));
    }

    /** Focuses the field without editing it. */
    public void focusField(ErpField field) {
        click(input(field));
    }

    public boolean isFieldEmpty(ErpField field) {
        String value = getAttribute(input(field), "value");
        return value == null || value.isEmpty();
    }

    /** The field's value. Never use this for the secret field's content. */
    public String getFieldValue(ErpField field) {
        String value = getAttribute(input(field), "value");
        return value == null ? "" : value;
    }

    public String getFieldInputType(ErpField field) {
        return getAttribute(input(field), "type");
    }

    /** The label text as displayed, including the " *" marker while the status is Active. */
    public String getFieldLabel(ErpField field) {
        return getText(locateElement("xpath", CreateBusinessUnitErpElements.labelXpath(field))).trim();
    }

    public boolean isFieldRequiredMarked(ErpField field) {
        return getFieldLabel(field).contains("*");
    }

    public boolean isFieldMarkedInvalid(ErpField field) {
        return "true".equals(getAttribute(input(field), "aria-invalid"));
    }

    /** Whether an error is currently shown for the field. The error element is removed when the field is valid. */
    public boolean isFieldErrorDisplayed(ErpField field) {
        return isPresent(CreateBusinessUnitErpElements.errorLocator(field));
    }

    /** Waits for the field's validation message, then returns its text. */
    public String getFieldError(ErpField field) {
        waitForElementVisible(CreateBusinessUnitErpElements.errorLocator(field));
        return getText(locateElement("xpath", CreateBusinessUnitErpElements.errorXpath(field))).trim();
    }

    public void waitForFieldErrorToDisappear(ErpField field) {
        waitForElementInvisible(CreateBusinessUnitErpElements.errorLocator(field));
    }

    /** True when aria-describedby is set and points at the field's own error message. */
    public boolean isFieldDescribedByItsError(ErpField field) {
        String describedBy = getAttribute(input(field), "aria-describedby");
        if (describedBy == null || describedBy.isBlank()) {
            return false;
        }
        WebElement error = locateElement("xpath", CreateBusinessUnitErpElements.errorXpath(field));
        return describedBy.equals(getAttribute(error, "id"));
    }

    /** True when the provider's aria-describedby is set and points at its own error message. */
    public boolean isProviderDescribedByItsError() {
        String describedBy = getAttribute(elements.getProviderInput(), "aria-describedby");
        return describedBy != null && !describedBy.isBlank()
                && describedBy.equals(getAttribute(elements.getProviderError(), "id"));
    }

    // ---------------------------------------------------------------- whole section

    /** The validation messages currently shown anywhere in the ERP section. */
    public List<String> getVisibleErrors() {
        List<String> messages = new ArrayList<>();
        for (WebElement error : elements.getPanelErrors()) {
            messages.add(getText(error).trim());
        }
        return messages;
    }

    // ----------------------------------------------------------------

    private WebElement input(ErpField field) {
        return locateElement("xpath", CreateBusinessUnitErpElements.inputXpath(field));
    }

    private List<String> optionTexts() {
        List<String> texts = new ArrayList<>();
        for (WebElement option : elements.getOptions()) {
            texts.add(getText(option).trim());
        }
        return texts;
    }
}
