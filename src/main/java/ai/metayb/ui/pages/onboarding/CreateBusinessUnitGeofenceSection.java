package ai.metayb.ui.pages.onboarding;

import ai.metayb.ui.components.onboarding.CreateBusinessUnitGeofenceElements;
import ai.metayb.ui.wrappers.BaseDriver;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The Geofence section of the Create Business Unit dialog. Use {@link CreateBusinessUnitDialog#selectSection} to bring
 * the section on screen first. Only unsaved form state is touched: there is no Create operation here.
 *
 * Validation here is edit-driven: an error is raised when Radius is edited, never by focus/blur or by changing the
 * status, and changing the status from Active to Inactive clears it. The "required" rule applies only while the status
 * is Active, whereas the whole-number and range rules apply in either status. Radius is a React-controlled text input,
 * so it is replaced with {@code replaceText} and emptied with {@code clearField}. The help text under the field is the
 * same paragraph that carries the error while the value is invalid.
 */
public class CreateBusinessUnitGeofenceSection extends BaseDriver {

    public static final String SECTION = "Geofence";
    public static final String ACTIVE = "Active";
    public static final String INACTIVE = "Inactive";

    private final CreateBusinessUnitGeofenceElements elements;

    public CreateBusinessUnitGeofenceSection() {
        super();
        elements = new CreateBusinessUnitGeofenceElements();
        PageFactory.initElements(getDriver(), elements);
    }

    // ---------------------------------------------------------------- static content

    public String getGeofenceHeading() {
        return getText(elements.getHeading()).trim();
    }

    public String getGeofenceDescription() {
        return getText(elements.getDescription()).trim();
    }

    public boolean isGeofenceSectionDisplayed() {
        return isDisplayed(elements.getHeading());
    }

    /** Moves focus away from any field by clicking the section heading. */
    public void blurGeofence() {
        click(elements.getHeading());
    }

    // ---------------------------------------------------------------- Geofence Config Status (MUI Select)

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

    /** The status list is the only list in this section, so any listbox on screen is it. */
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
        List<String> texts = new ArrayList<>();
        for (WebElement option : elements.getOptions()) {
            texts.add(getText(option).trim());
        }
        return texts;
    }

    /** Whether this option is marked selected (aria-selected) in the open list. */
    public boolean isConfigStatusOptionSelected(String status) {
        waitForElementVisible(CreateBusinessUnitGeofenceElements.statusOptionLocator(status));
        WebElement option = locateElement("xpath", CreateBusinessUnitGeofenceElements.statusOptionXpath(status));
        return "true".equals(getAttribute(option, "aria-selected"));
    }

    /** Opens the list if it is closed, clicks the option and waits for the list to close and the select to show it. */
    public void selectConfigStatus(String status) {
        if (!isConfigStatusListOpen()) {
            openConfigStatusList();
        }
        waitForElementClickable(CreateBusinessUnitGeofenceElements.statusOptionLocator(status));
        click(locateElement("xpath", CreateBusinessUnitGeofenceElements.statusOptionXpath(status)));
        waitForElementInvisible(elements.getListboxLocator());
        if (!status.equals(getConfigStatus())) {
            reportStep("Expected Geofence Config Status '" + status + "' but the select shows '" + getConfigStatus() + "'", "FAIL");
        }
    }

    // ---------------------------------------------------------------- Radius

    /** Replaces the radius (keyboard clear, then type), as the field is React-controlled. */
    public void enterRadius(String radius) {
        replaceText(elements.getRadiusInput(), radius);
    }

    /** Types after the existing text without clearing, to test what the field accepts beyond its limit. */
    public void appendToRadius(String text) {
        typeWithoutClear(elements.getRadiusInput(), text);
    }

    /** Empties the radius with key presses so the form's own validation sees the edit. */
    public void clearRadius() {
        clearField(elements.getRadiusInput());
    }

    /** Focuses the radius without editing it. */
    public void focusRadius() {
        click(elements.getRadiusInput());
    }

    public String getRadius() {
        String value = getAttribute(elements.getRadiusInput(), "value");
        return value == null ? "" : value;
    }

    public boolean isRadiusEmpty() {
        return getRadius().isEmpty();
    }

    /** The label as displayed, including the " *" marker while the status is Active. */
    public String getRadiusLabel() {
        return getText(elements.getRadiusLabel()).trim();
    }

    public boolean isRadiusRequiredMarked() {
        return getRadiusLabel().contains("*");
    }

    /** The unit shown at the end of the field ("m"). */
    public String getRadiusUnit() {
        return getText(elements.getRadiusUnit()).trim();
    }

    /** The text under the field: the help text normally, the error message while the value is invalid. */
    public String getRadiusHelpText() {
        return getText(elements.getRadiusMessage()).trim();
    }

    public String getRadiusInputType() {
        return getAttribute(elements.getRadiusInput(), "type");
    }

    public String getRadiusInputMode() {
        return getAttribute(elements.getRadiusInput(), "inputmode");
    }

    public String getRadiusPattern() {
        return getAttribute(elements.getRadiusInput(), "pattern");
    }

    /** The input's maxlength, or -1 when it has none. */
    public int getMaxLength() {
        String value = getAttribute(elements.getRadiusInput(), "maxlength");
        return value == null || value.isBlank() ? -1 : Integer.parseInt(value.trim());
    }

    public boolean isRadiusMarkedInvalid() {
        return "true".equals(getAttribute(elements.getRadiusInput(), "aria-invalid"));
    }

    /** Whether an error is currently shown for the radius. The error styling is removed when the value is valid. */
    public boolean isRadiusErrorDisplayed() {
        return isPresent(elements.getRadiusErrorLocator());
    }

    /** Waits for the radius's validation message, then returns its text. */
    public String getRadiusError() {
        waitForElementVisible(elements.getRadiusErrorLocator());
        return getText(elements.getRadiusError()).trim();
    }

    public void waitForRadiusErrorToDisappear() {
        waitForElementInvisible(elements.getRadiusErrorLocator());
    }

    /** True when aria-describedby is set and points at the paragraph under the field (help text or error). */
    public boolean isRadiusDescribedByItsMessage() {
        String describedBy = getAttribute(elements.getRadiusInput(), "aria-describedby");
        return describedBy != null && !describedBy.isBlank()
                && describedBy.equals(getAttribute(elements.getRadiusMessage(), "id"));
    }

    // ---------------------------------------------------------------- whole section

    /** The validation messages currently shown anywhere in the Geofence section. */
    public List<String> getVisibleErrors() {
        List<String> messages = new ArrayList<>();
        for (WebElement error : elements.getPanelErrors()) {
            messages.add(getText(error).trim());
        }
        return messages;
    }
}
