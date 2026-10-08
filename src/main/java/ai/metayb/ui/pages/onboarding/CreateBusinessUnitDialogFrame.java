package ai.metayb.ui.pages.onboarding;

import ai.metayb.ui.components.onboarding.CreateBusinessUnitFrameElements;
import ai.metayb.ui.wrappers.BaseDriver;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * The frame of the Create Business Unit dialog: its modal attributes, title chips, and what happens when the user
 * presses Escape or clicks outside. Opening, closing, section navigation and the Business Unit Details field stay in
 * {@link CreateBusinessUnitDialog}. Like that class, this one can only read and attempt to dismiss: it has no way to
 * click Create, so nothing can be persisted through it.
 */
public class CreateBusinessUnitDialogFrame extends BaseDriver {

    private final CreateBusinessUnitFrameElements elements;

    public CreateBusinessUnitDialogFrame() {
        super();
        elements = new CreateBusinessUnitFrameElements();
        PageFactory.initElements(getDriver(), elements);
    }

    // ---------------------------------------------------------------- modal attributes and title

    public boolean isOpen() {
        return isPresent(elements.getDialogLocator());
    }

    public String getRole() {
        return getAttribute(elements.getDialog(), "role");
    }

    public boolean isModal() {
        return "true".equals(getAttribute(elements.getDialog(), "aria-modal"));
    }

    public String getTitleText() {
        return getText(elements.getTitle()).trim();
    }

    public boolean isNewChipDisplayed() {
        return isDisplayed(elements.getNewChip());
    }

    // ---------------------------------------------------------------- "Unsaved changes" chip

    /** Whether the chip is on screen. It exists only while some value differs from its default. */
    public boolean isUnsavedChangesChipDisplayed() {
        return isPresent(elements.getUnsavedChipLocator());
    }

    public void waitForUnsavedChangesChip() {
        waitForElementVisible(elements.getUnsavedChipLocator());
    }

    public void waitForUnsavedChangesChipToDisappear() {
        waitForElementInvisible(elements.getUnsavedChipLocator());
    }

    // ---------------------------------------------------------------- dismiss attempts the portal ignores

    /** Presses the Escape key, which goes to the focused element inside the open dialog. */
    public void pressEscape() {
        try {
            new Actions(getDriver()).sendKeys(Keys.ESCAPE).perform();
            reportStep("The Escape key is pressed", "PASS");
        } catch (Exception e) {
            reportStep("The Escape key could not be pressed: " + e.getMessage(), "FAIL");
        }
    }

    /**
     * Clicks outside the dialog. MUI handles that click on its full-viewport container, not on the visual backdrop, so
     * the container is the target: a real mouse click near its corner, which lies outside the dialog, is what a user's
     * click on the backdrop reaches. The point is derived from the container's own size, not hard-coded.
     */
    public void clickOutsideDialog() {
        try {
            WebElement container = elements.getContainer();
            Dimension size = container.getSize();
            new Actions(getDriver())
                    .moveToElement(container, 5 - size.getWidth() / 2, 5 - size.getHeight() / 2)
                    .click()
                    .perform();
            reportStep("The area outside the dialog is clicked", "PASS");
        } catch (Exception e) {
            reportStep("The area outside the dialog could not be clicked: " + e.getMessage(), "FAIL");
        }
    }

    /**
     * Whether the dialog is still on screen for this long. A closing MUI dialog stays in the DOM through its exit
     * transition, so a presence check straight after an action could pass although the dialog is closing; waiting for
     * it to disappear and seeing that it does not is what shows it really stays open.
     */
    public boolean staysOpenFor(int seconds) {
        try {
            new WebDriverWait(getDriver(), Duration.ofSeconds(seconds))
                    .until(ExpectedConditions.invisibilityOfElementLocated(elements.getDialogLocator()));
            reportStep("The dialog disappeared", "INFO");
            return false;
        } catch (TimeoutException e) {
            reportStep("The dialog stayed on screen for " + seconds + "s", "INFO");
            return true;
        }
    }

    // ---------------------------------------------------------------- confirmations

    /** Whether any confirmation (an alert dialog, a SweetAlert popup or another dialog) is on screen. */
    public boolean isConfirmationDisplayed() {
        return isPresent(elements.getConfirmationLocator());
    }
}
