package ai.metayb.ui.pages.onboarding;

import ai.metayb.ui.components.onboarding.CreateBusinessUnitPaymentElements;
import ai.metayb.ui.wrappers.BaseDriver;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The Payment Integration section of the Create Business Unit dialog. Use {@link CreateBusinessUnitDialog#selectSection}
 * to bring the section on screen first. Only unsaved form state is touched: there is no Create operation here.
 *
 * Validation in this section is edit-driven and only applies while Payment Config Status is Active: a required error
 * appears when the provider or the secret key is edited to empty, not on focus/blur and not when the status changes.
 * Fields are emptied with {@code clearField} (key presses) because they are React-controlled.
 * The PAYSTACK secret key goes through {@code typeSecret}; its value is never read back or logged, and the only things
 * exposed about it are whether it is empty and its input type (password while hidden, text while revealed).
 */
public class CreateBusinessUnitPaymentSection extends BaseDriver {

    public static final String SECTION = "Payment Integration";
    public static final String ACTIVE = "Active";
    public static final String INACTIVE = "Inactive";
    public static final String PAYSTACK = "Paystack";

    private final CreateBusinessUnitPaymentElements elements;

    public CreateBusinessUnitPaymentSection() {
        super();
        elements = new CreateBusinessUnitPaymentElements();
        PageFactory.initElements(getDriver(), elements);
    }

    // ---------------------------------------------------------------- static content

    public String getPaymentHeading() {
        return getText(elements.getHeading()).trim();
    }

    public String getPaymentDescription() {
        return getText(elements.getDescription()).trim();
    }

    public boolean isPaymentSectionDisplayed() {
        return isDisplayed(elements.getHeading());
    }

    /** Moves focus away from any field by clicking the section heading. */
    public void blurPayment() {
        click(elements.getHeading());
    }

    // ---------------------------------------------------------------- Payment Config Status (MUI Select)

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

    /** Whether the status list is open. Its options carry data-value, which the provider's options do not. */
    public boolean isConfigStatusListOpen() {
        return isPresent(CreateBusinessUnitPaymentElements.statusOptionLocator(ACTIVE));
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
        waitForElementVisible(CreateBusinessUnitPaymentElements.statusOptionLocator(status));
        WebElement option = locateElement("xpath", CreateBusinessUnitPaymentElements.statusOptionXpath(status));
        return "true".equals(getAttribute(option, "aria-selected"));
    }

    /** Opens the list if needed, clicks the option and waits for the list to close and the select to show it. */
    public void selectConfigStatus(String status) {
        if (isProviderListOpen()) {
            closeProviderListWithEscape();
        }
        if (!isConfigStatusListOpen()) {
            openConfigStatusList();
        }
        waitForElementClickable(CreateBusinessUnitPaymentElements.statusOptionLocator(status));
        click(locateElement("xpath", CreateBusinessUnitPaymentElements.statusOptionXpath(status)));
        waitForElementInvisible(elements.getListboxLocator());
        if (!status.equals(getConfigStatus())) {
            reportStep("Expected Payment Config Status '" + status + "' but the select shows '" + getConfigStatus() + "'", "FAIL");
        }
    }

    // ---------------------------------------------------------------- Payment Provider (Autocomplete)

    /** The text in the provider box, e.g. "Paystack", or an empty string. */
    public String getProvider() {
        String value = getAttribute(elements.getProviderInput(), "value");
        return value == null ? "" : value;
    }

    /** Opens the provider list with a click. Only call this when the list is closed: clicking an open, empty one closes it. */
    public void openProviderList() {
        click(elements.getProviderInput());
        waitForElementVisible(elements.getListboxLocator());
    }

    /** Opens the provider list with ArrowDown on the focused input (the first press opens it without highlighting). */
    public void openProviderListWithKeyboard() {
        typeWithoutClear(elements.getProviderInput(), Keys.ARROW_DOWN.toString());
        waitForElementVisible(elements.getListboxLocator());
    }

    /** Whether the provider list is open: a listbox is on screen and it is not the status list. */
    public boolean isProviderListOpen() {
        return isPresent(elements.getListboxLocator()) && !isConfigStatusListOpen();
    }

    /** Closes only the list with Escape; the typed text or value and the dialog stay. */
    public void closeProviderListWithEscape() {
        typeWithoutClear(elements.getProviderInput(), Keys.ESCAPE.toString());
        waitForElementInvisible(elements.getListboxLocator());
    }

    /**
     * Picks the option for this provider by its visible text. The list may already be open (after reading its options,
     * or after typing or clearing with the keyboard), and clicking an empty, open Autocomplete closes it, so it is only
     * opened when closed.
     */
    public void selectProvider(String provider) {
        if (!isProviderListOpen()) {
            openProviderList();
        }
        waitForElementClickable(CreateBusinessUnitPaymentElements.providerOptionLocator(provider));
        click(locateElement("xpath", CreateBusinessUnitPaymentElements.providerOptionXpath(provider)));
        waitForElementInvisible(elements.getListboxLocator());
        if (!provider.equals(getProvider())) {
            reportStep("Expected Payment Provider '" + provider + "' but the field shows '" + getProvider() + "'", "FAIL");
        }
    }

    /**
     * Selects the first option with the keyboard: ArrowDown highlights it and Enter picks it. The list must be open
     * (see {@link #openProviderListWithKeyboard}), since the press that opens the list does not highlight anything.
     */
    public void selectHighlightedProviderWithKeyboard() {
        typeWithoutClear(elements.getProviderInput(), Keys.ARROW_DOWN.toString());
        typeWithoutClear(elements.getProviderInput(), Keys.ENTER.toString());
        waitForElementInvisible(elements.getListboxLocator());
    }

    /** Replaces the text in the search box (the list opens as you type). Does not select anything. */
    public void searchProvider(String text) {
        replaceText(elements.getProviderInput(), text);
    }

    /** Empties the provider with key presses, which removes the selection the way a user would. */
    public void clearProviderWithKeyboard() {
        clearField(elements.getProviderInput());
    }

    /**
     * Empties the provider with its Clear (X) button, which MUI shows while the box has a value and focus or hover, so
     * call this right after selecting. The list stays closed afterwards.
     */
    public void clearProviderWithButton() {
        waitForElementClickable(elements.getProviderClearButtonLocator());
        click(elements.getProviderClearButton());
        if (!getProvider().isEmpty()) {
            reportStep("Expected the Clear button to empty the provider but it shows '" + getProvider() + "'", "FAIL");
        }
    }

    /** The option texts of the open provider list. Waits for the list. */
    public List<String> getProviderOptions() {
        waitForElementVisible(elements.getListboxLocator());
        return optionTexts();
    }

    public boolean isProviderOptionListed(String provider) {
        return isPresent(CreateBusinessUnitPaymentElements.providerOptionLocator(provider));
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

    /** True when the provider's aria-describedby is set and points at its own error message. */
    public boolean isProviderDescribedByItsError() {
        String describedBy = getAttribute(elements.getProviderInput(), "aria-describedby");
        return describedBy != null && !describedBy.isBlank()
                && describedBy.equals(getAttribute(elements.getProviderError(), "id"));
    }

    // ---------------------------------------------------------------- PAYSTACK Secret Key

    /** Replaces the secret: cleared by key presses, then typed with typeSecret so the value never reaches the log. */
    public void enterSecretKey(String fakeValue) {
        clearField(elements.getSecretKeyInput());
        typeSecret(elements.getSecretKeyInput(), fakeValue);
    }

    /** Empties the secret with key presses so the form's own validation sees the edit. */
    public void clearSecretKey() {
        clearField(elements.getSecretKeyInput());
    }

    /** Focuses the secret without editing it. */
    public void focusSecretKey() {
        click(elements.getSecretKeyInput());
    }

    /**
     * Whether the secret is empty. The wrapper's getAttribute masks a value only while the input type is password, so
     * once the eye reveals the field it would log the secret. The value is therefore read here without logging and only
     * "empty" or "filled" is reported.
     */
    public boolean isSecretKeyEmpty() {
        boolean empty;
        try {
            String value = elements.getSecretKeyInput().getDomProperty("value");
            empty = value == null || value.isEmpty();
        } catch (Exception e) {
            reportStep("Could not tell whether the PAYSTACK Secret Key is empty: " + e.getMessage(), "FAIL");
            return false;
        }
        reportStep("The PAYSTACK Secret Key is " + (empty ? "empty" : "filled"), "INFO");
        return empty;
    }

    /** "password" while the secret is masked, "text" while it is revealed. */
    public String getSecretKeyInputType() {
        return getAttribute(elements.getSecretKeyInput(), "type");
    }

    /** Clicks the eye button next to the secret, which toggles masking. */
    public void toggleSecretKeyVisibility() {
        waitForElementClickable(elements.getSecretKeyEyeButtonLocator());
        click(elements.getSecretKeyEyeButton());
    }

    /** The label as displayed, including the " *" marker while the status is Active. */
    public String getSecretKeyLabel() {
        return getText(elements.getSecretKeyLabel()).trim();
    }

    public boolean isSecretKeyRequiredMarked() {
        return getSecretKeyLabel().contains("*");
    }

    public boolean isSecretKeyMarkedInvalid() {
        return "true".equals(getAttribute(elements.getSecretKeyInput(), "aria-invalid"));
    }

    /** Whether an error is currently shown for the secret. The error element is removed when it is valid. */
    public boolean isSecretKeyErrorDisplayed() {
        return isPresent(elements.getSecretKeyErrorLocator());
    }

    /** Waits for the secret's validation message, then returns its text. */
    public String getSecretKeyError() {
        waitForElementVisible(elements.getSecretKeyErrorLocator());
        return getText(elements.getSecretKeyError()).trim();
    }

    public void waitForSecretKeyErrorToDisappear() {
        waitForElementInvisible(elements.getSecretKeyErrorLocator());
    }

    /** True when aria-describedby is set and points at the secret's own error message. */
    public boolean isSecretKeyDescribedByItsError() {
        String describedBy = getAttribute(elements.getSecretKeyInput(), "aria-describedby");
        return describedBy != null && !describedBy.isBlank()
                && describedBy.equals(getAttribute(elements.getSecretKeyError(), "id"));
    }

    // ---------------------------------------------------------------- whole section

    /** The validation messages currently shown anywhere in the Payment section. */
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
