package ai.metayb.ui.pages.onboarding;

import ai.metayb.ui.components.onboarding.CreateBusinessUnitDialogElements;
import ai.metayb.ui.wrappers.BaseDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * The Create Business Unit dialog, which DashboardPage.clickAddBusinessUnit() opens. This class only reads,
 * edits unsaved form text and navigates/closes: there is intentionally no method that clicks Create or uploads
 * a logo, so nothing can be persisted through it.
 */
public class CreateBusinessUnitDialog extends BaseDriver {

    public static final String BUSINESS_UNIT_DETAILS = "Business Unit Details";
    public static final List<String> SECTIONS = List.of(BUSINESS_UNIT_DETAILS, "Currency", "ERP Integration",
            "Payment Integration", "CDP Integration", "OTP Verification", "Geofence", "Activity Communication");

    private static final String DASHBOARD_PATH = "/dashboard";

    private final CreateBusinessUnitDialogElements elements;

    public CreateBusinessUnitDialog() {
        super();
        elements = new CreateBusinessUnitDialogElements();
        PageFactory.initElements(getDriver(), elements);
    }

    // ---------------------------------------------------------------- dialog

    public void waitForDialog() {
        waitForElementVisible(elements.getDialogLocator());
    }

    public boolean isDialogDisplayed() {
        return isPresent(elements.getDialogLocator());
    }

    /** The title text, e.g. "Create Business Unit" followed by its "New" chip. */
    public String getTitle() {
        return getText(elements.getTitle()).replaceAll("\\s+", " ").trim();
    }

    public boolean isCreateButtonEnabled() {
        return isEnabled(elements.getCreateButton());
    }

    /** The dialog opens over the dashboard without changing the route. */
    public boolean isOnDashboardRoute() {
        return DASHBOARD_PATH.equals(URI.create(getCurrentUrl()).getPath());
    }

    public void cancel() {
        click(elements.getCancelButton());
        waitForElementInvisible(elements.getDialogLocator());
    }

    public void closeWithX() {
        click(elements.getCloseButton());
        waitForElementInvisible(elements.getDialogLocator());
    }

    public void closeWithBackArrow() {
        click(elements.getBackArrowButton());
        waitForElementInvisible(elements.getDialogLocator());
    }

    // ---------------------------------------------------------------- section navigation

    public List<String> getSectionNames() {
        List<String> names = new ArrayList<>();
        for (WebElement row : elements.getNavRows()) {
            names.add(getText(row).trim());
        }
        return names;
    }

    public String getSelectedSection() {
        waitForElementVisible(elements.getSelectedNavRowLocator());
        return getText(elements.getSelectedNavRow()).trim();
    }

    public boolean isBusinessUnitDetailsSelected() {
        return BUSINESS_UNIT_DETAILS.equals(getSelectedSection());
    }

    /** Clicks the section in the left navigation and waits until it is the selected one and its panel is shown. */
    public void selectSection(String section) {
        click(locateElement("xpath", CreateBusinessUnitDialogElements.navRowXpath(section)));
        waitForElementVisible(CreateBusinessUnitDialogElements.selectedNavRowFor(section));
        waitForElementVisible(org.openqa.selenium.By.xpath(CreateBusinessUnitDialogElements.sectionHeadingXpath(section)));
    }

    /**
     * Every panel stays mounted, so this uses isDisplayed (not isPresent) to say whether the section's panel is
     * the one actually on screen.
     */
    public boolean isSectionDisplayed(String section) {
        return isDisplayed(locateElement("xpath", CreateBusinessUnitDialogElements.sectionHeadingXpath(section)));
    }

    // ---------------------------------------------------------------- Business Unit Details panel

    public String getSectionHeading() {
        return getText(elements.getSectionHeading()).trim();
    }

    public String getSectionDescription() {
        return getText(elements.getSectionDescription()).trim();
    }

    public boolean isNameFieldDisplayed() {
        return isDisplayed(elements.getNameInput());
    }

    /**
     * Replaces the text. Not type(): its clear() is undone by React, which would leave the old value in front of the
     * new text. Use {@link #clearBusinessUnitName()} to leave the field empty.
     */
    public void enterBusinessUnitName(String name) {
        replaceText(elements.getNameInput(), name);
    }

    public void appendToBusinessUnitName(String text) {
        typeWithoutClear(elements.getNameInput(), text);
    }

    /** Empties the field with key presses so the form's own validation sees the edit (see Element#clearField). */
    public void clearBusinessUnitName() {
        clearField(elements.getNameInput());
    }

    public String getBusinessUnitName() {
        String value = getAttribute(elements.getNameInput(), "value");
        return value == null ? "" : value;
    }

    /** Focuses the field without editing it. */
    public void focusBusinessUnitName() {
        click(elements.getNameInput());
    }

    /** Moves focus away from the field by clicking the section heading. */
    public void blurBusinessUnitName() {
        click(elements.getSectionHeading());
    }

    public boolean isNameErrorDisplayed() {
        return isDisplayed(elements.getNameError());
    }

    /** Waits for the validation message to appear, then returns its text. */
    public String getNameError() {
        waitForElementVisible(elements.getNameErrorLocator());
        return getText(elements.getNameError()).trim();
    }

    public void waitForNameErrorToDisappear() {
        waitForElementInvisible(elements.getNameErrorLocator());
    }

    // ---------------------------------------------------------------- Logo (read-only: never click Add Logo)

    public boolean isLogoSectionDisplayed() {
        return isDisplayed(elements.getLogoCaption()) && isDisplayed(elements.getAddLogoLabel());
    }

    public String getLogoHelpText() {
        return getText(elements.getLogoHelpText()).trim();
    }

    /** The hidden file input's accept attribute, read without interacting with it. */
    public String getAcceptedLogoFileTypes() {
        return getAttribute(elements.getLogoFileInput(), "accept");
    }
}
