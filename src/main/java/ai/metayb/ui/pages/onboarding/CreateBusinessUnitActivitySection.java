package ai.metayb.ui.pages.onboarding;

import ai.metayb.ui.components.onboarding.CreateBusinessUnitActivityElements;
import ai.metayb.ui.wrappers.BaseDriver;
import ai.metayb.ui.wrappers.SensitiveData;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The Activity Communication section of the Create Business Unit dialog. Use {@link CreateBusinessUnitDialog#selectSection}
 * to bring the section on screen first. Only unsaved form state is touched: there is no Create operation here.
 *
 * The section is one accordion ("Acknowledgement Communication") with Status and Channel selects; choosing the channel
 * reveals API Key (a secret) and Sender ID (optional). The required rule applies only while Status is Active and,
 * like the other sections, validation is edit-driven: an error appears when API Key is edited to empty, never from
 * focus/blur or from a status change, and Active to Inactive clears it while keeping every value.
 *
 * The API key goes through {@code clearField} + {@code typeSecret} and is never read through the logging
 * {@code getAttribute}; its emptiness is read straight from the DOM property and only "empty" or "filled" is reported.
 * Controls that are hidden by collapsing, or not yet revealed, are judged by what the user can see (displayed), not by
 * their presence in the DOM.
 */
public class CreateBusinessUnitActivitySection extends BaseDriver {

    public static final String SECTION = "Activity Communication";
    public static final String ACTIVE = "Active";
    public static final String INACTIVE = "Inactive";
    public static final String INFINITY_CONNECT = "Infinity Connect";
    /** The machine value the portal holds for the Infinity Connect channel. */
    public static final String INFINITY_CONNECT_VALUE = "INFINITY_CONNECT";

    private final CreateBusinessUnitActivityElements elements;

    public CreateBusinessUnitActivitySection() {
        super();
        elements = new CreateBusinessUnitActivityElements();
        PageFactory.initElements(getDriver(), elements);
    }

    // ---------------------------------------------------------------- static content

    public String getActivityHeading() {
        return getText(elements.getHeading()).trim();
    }

    public String getActivityDescription() {
        return getText(elements.getDescription()).trim();
    }

    public boolean isActivitySectionDisplayed() {
        return isDisplayed(elements.getHeading());
    }

    /** Moves focus away from any field by clicking the section heading. */
    public void blurActivity() {
        click(elements.getHeading());
    }

    // ---------------------------------------------------------------- accordion

    public String getAccordionTitle() {
        return getText(elements.getAccordionTitle()).trim();
    }

    public String getAccordionDescription() {
        return getText(elements.getAccordionDescription()).trim();
    }

    public boolean isAccordionExpanded() {
        return "true".equals(getAttribute(elements.getAccordionHeader(), "aria-expanded"));
    }

    /** Collapses the accordion and waits until its controls are hidden from the user (not merely removed from view). */
    public void collapseAccordion() {
        if (isAccordionExpanded()) {
            click(elements.getAccordionHeader());
        }
        waitForElementInvisible(elements.getStatusSelectLocator());
    }

    /** Expands the accordion and waits until its controls are visible. */
    public void expandAccordion() {
        if (!isAccordionExpanded()) {
            click(elements.getAccordionHeader());
        }
        waitForElementVisible(elements.getStatusSelectLocator());
    }

    /** Whether the header shows its error icon. The icon has no label; it exists only while the section has an error. */
    public boolean isAccordionErrorIconDisplayed() {
        return isShown(elements.getAccordionErrorIconLocator());
    }

    // ---------------------------------------------------------------- Status (MUI Select)

    /** The text the select shows ("Active" / "Inactive"). */
    public String getStatus() {
        return getText(elements.getStatusSelect()).trim();
    }

    /** The machine value held by the select's hidden input ("active" / "inactive"). */
    public String getStatusValue() {
        String value = getAttribute(elements.getStatusValueInput(), "value");
        return value == null ? "" : value;
    }

    public boolean isStatusDisplayed() {
        return isShown(elements.getStatusSelectLocator());
    }

    public boolean isStatusRequiredMarked() {
        return getText(elements.getStatusLabel()).contains("*");
    }

    public void openStatusList() {
        click(elements.getStatusSelect());
        waitForElementVisible(elements.getListboxLocator());
    }

    /** Whether the Status list is open. Its options carry data-value active/inactive. */
    public boolean isStatusListOpen() {
        return isPresent(CreateBusinessUnitActivityElements.statusOptionLocator(ACTIVE));
    }

    public void closeStatusListWithEscape() {
        typeWithoutClear(elements.getListbox(), Keys.ESCAPE.toString());
        waitForElementInvisible(elements.getListboxLocator());
    }

    /** The Status option texts, opening the list first if it is closed. The list is left open. */
    public List<String> getStatusOptions() {
        if (!isStatusListOpen()) {
            openStatusList();
        }
        return optionTexts();
    }

    public boolean isStatusOptionSelected(String status) {
        waitForElementVisible(CreateBusinessUnitActivityElements.statusOptionLocator(status));
        WebElement option = locateElement("xpath", CreateBusinessUnitActivityElements.statusOptionXpath(status));
        return "true".equals(getAttribute(option, "aria-selected"));
    }

    /** Opens the list if it is closed, clicks the option and waits for the list to close and the select to show it. */
    public void selectStatus(String status) {
        dismissOpenList();
        if (!isStatusListOpen()) {
            openStatusList();
        }
        waitForElementClickable(CreateBusinessUnitActivityElements.statusOptionLocator(status));
        click(locateElement("xpath", CreateBusinessUnitActivityElements.statusOptionXpath(status)));
        waitForElementInvisible(elements.getListboxLocator());
        if (!status.equals(getStatus())) {
            reportStep("Expected Status '" + status + "' but the select shows '" + getStatus() + "'", "FAIL");
        }
    }

    // ---------------------------------------------------------------- Channel (MUI Select)

    /** The text the select shows, or an empty string while nothing is chosen (the empty select holds a zero-width space). */
    public String getChannel() {
        String shown = getText(elements.getChannelSelect());
        return shown.replaceAll("[\\u200B\\s]", "").isEmpty() ? "" : shown.trim();
    }

    /** The machine value held by the select's hidden input ("" or "INFINITY_CONNECT"). */
    public String getChannelValue() {
        String value = getAttribute(elements.getChannelValueInput(), "value");
        return value == null ? "" : value;
    }

    public boolean isChannelDisplayed() {
        return isShown(elements.getChannelSelectLocator());
    }

    public void openChannelList() {
        click(elements.getChannelSelect());
        waitForElementVisible(elements.getListboxLocator());
    }

    /** Whether the Channel list is open: a list is on screen and it is not the Status list. */
    public boolean isChannelListOpen() {
        return isPresent(elements.getListboxLocator()) && !isStatusListOpen();
    }

    public void closeChannelListWithEscape() {
        typeWithoutClear(elements.getListbox(), Keys.ESCAPE.toString());
        waitForElementInvisible(elements.getListboxLocator());
    }

    /** The Channel option texts, opening the list first if it is closed. The list is left open. */
    public List<String> getChannelOptions() {
        if (!isChannelListOpen()) {
            openChannelList();
        }
        return optionTexts();
    }

    /** Opens the list if it is closed, clicks the option and waits for the list to close and the select to show it. */
    public void selectChannel(String channel) {
        dismissOpenList();
        if (!isChannelListOpen()) {
            openChannelList();
        }
        waitForElementClickable(CreateBusinessUnitActivityElements.channelOptionLocator(channel));
        click(locateElement("xpath", CreateBusinessUnitActivityElements.channelOptionXpath(channel)));
        waitForElementInvisible(elements.getListboxLocator());
        if (!channel.equals(getChannel())) {
            reportStep("Expected Channel '" + channel + "' but the select shows '" + getChannel() + "'", "FAIL");
        }
    }

    // ---------------------------------------------------------------- API Key (secret)

    public boolean isApiKeyDisplayed() {
        return isShown(elements.getApiKeyInputLocator());
    }

    public String getApiKeyLabel() {
        return getText(elements.getApiKeyLabel()).trim();
    }

    public boolean isApiKeyRequiredMarked() {
        return getApiKeyLabel().contains("*");
    }

    /** "password" while the key is masked, "text" while it is revealed. */
    public String getApiKeyInputType() {
        return getAttribute(elements.getApiKeyInput(), "type");
    }

    /** Replaces the key with a fake value, typed with typeSecret so it is never logged. */
    public void enterApiKey(String fakeKey) {
        clearField(elements.getApiKeyInput());
        typeSecret(elements.getApiKeyInput(), fakeKey);
    }

    /** Empties the key with key presses so the form's own validation sees the edit. */
    public void clearApiKey() {
        clearField(elements.getApiKeyInput());
    }

    public void focusApiKey() {
        click(elements.getApiKeyInput());
    }

    /** Whether the key field is empty. Read without logging; only "empty" or "filled" is reported. */
    public boolean isApiKeyEmpty() {
        boolean empty;
        try {
            String value = elements.getApiKeyInput().getDomProperty("value");
            empty = value == null || value.isEmpty();
        } catch (Exception e) {
            reportStep("Could not tell whether the API key is empty: " + e.getMessage(), "FAIL");
            return false;
        }
        reportStep("The API key is " + (empty ? "empty" : "filled"), "INFO");
        return empty;
    }

    /** Clicks the eye button, which switches the key between password and text. */
    public void toggleApiKeyVisibility() {
        waitForElementClickable(elements.getApiKeyEyeButtonLocator());
        click(elements.getApiKeyEyeButton());
    }

    public boolean isApiKeyMarkedInvalid() {
        return "true".equals(getAttribute(elements.getApiKeyInput(), "aria-invalid"));
    }

    public boolean isApiKeyErrorDisplayed() {
        return isPresent(elements.getApiKeyErrorLocator());
    }

    /** Waits for the API key's validation message, then returns its text. */
    public String getApiKeyError() {
        waitForElementVisible(elements.getApiKeyErrorLocator());
        return getText(elements.getApiKeyError()).trim();
    }

    public void waitForApiKeyErrorToDisappear() {
        waitForElementInvisible(elements.getApiKeyErrorLocator());
    }

    /** True when aria-describedby is set and points at the key's own error message. */
    public boolean isApiKeyDescribedByItsError() {
        String describedBy = getAttribute(elements.getApiKeyInput(), "aria-describedby");
        return describedBy != null && !describedBy.isBlank()
                && describedBy.equals(getAttribute(elements.getApiKeyError(), "id"));
    }

    /**
     * Whether this text appears in the page source that failure diagnostics would attach, which is the scrubbed one:
     * false means the text, a secret, is not in it. Only the answer is returned, never the source.
     */
    public boolean isInDiagnosticPageSource(String text) {
        return SensitiveData.scrubPageSource(getDriver().getPageSource()).contains(text);
    }

    // ---------------------------------------------------------------- Sender ID (optional, plain text)

    public boolean isSenderIdDisplayed() {
        return isShown(elements.getSenderIdInputLocator());
    }

    public String getSenderIdLabel() {
        return getText(elements.getSenderIdLabel()).trim();
    }

    public boolean isSenderIdRequiredMarked() {
        return getSenderIdLabel().contains("*");
    }

    /** Replaces the Sender ID, as the field is React-controlled. */
    public void enterSenderId(String senderId) {
        replaceText(elements.getSenderIdInput(), senderId);
    }

    public void clearSenderId() {
        clearField(elements.getSenderIdInput());
    }

    public void focusSenderId() {
        click(elements.getSenderIdInput());
    }

    public String getSenderId() {
        String value = getAttribute(elements.getSenderIdInput(), "value");
        return value == null ? "" : value;
    }

    public boolean isSenderIdMarkedInvalid() {
        return "true".equals(getAttribute(elements.getSenderIdInput(), "aria-invalid"));
    }

    public boolean isSenderIdErrorDisplayed() {
        return isPresent(elements.getSenderIdErrorLocator());
    }

    // ---------------------------------------------------------------- whole section

    /** The validation messages currently shown anywhere in the Activity Communication section. */
    public List<String> getVisibleErrors() {
        List<String> messages = new ArrayList<>();
        for (WebElement error : elements.getPanelErrors()) {
            messages.add(getText(error).trim());
        }
        return messages;
    }

    // ----------------------------------------------------------------

    /** Whether an element that may not exist yet is on screen: present and displayed, never an error if absent. */
    private boolean isShown(By locator) {
        List<WebElement> found = getDriver().findElements(locator);
        return !found.isEmpty() && isDisplayed(found.get(0));
    }

    /** Closes a list left open by an earlier action (Status and Channel share one list on screen at a time). */
    private void dismissOpenList() {
        if (isPresent(elements.getListboxLocator())) {
            typeWithoutClear(elements.getListbox(), Keys.ESCAPE.toString());
            waitForElementInvisible(elements.getListboxLocator());
        }
    }

    private List<String> optionTexts() {
        List<String> texts = new ArrayList<>();
        for (WebElement option : elements.getOptions()) {
            texts.add(getText(option).trim());
        }
        return texts;
    }
}
