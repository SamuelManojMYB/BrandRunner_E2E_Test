package ai.metayb.ui.pages.onboarding;

import ai.metayb.ui.components.onboarding.TenantCronsElements;
import ai.metayb.ui.wrappers.BaseDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Read-only view of the Tenant Crons dialog. Opening it, closing it and listing the cron names and statuses stay in
 * {@link DashboardPage} ({@code openTenantCrons}, {@code waitForTenantCronEntries}, {@code getTenantCronNames},
 * {@code getTenantCronStatuses}, {@code closeTenantCrons}); this class adds what the Dashboard does not cover.
 *
 * SAFETY: this class has no method that clicks, submits or otherwise triggers a cron's Remove button or an Add cron
 * button, and none may be added. A cron's Remove button is only ever read (present, enabled, its text). The dialog
 * acts on a click straight away (Remove has no confirmation), so removing a cron by accident would be real.
 */
public class TenantCronsDialog extends BaseDriver {

    private final TenantCronsElements elements;

    public TenantCronsDialog() {
        super();
        elements = new TenantCronsElements();
        PageFactory.initElements(getDriver(), elements);
    }

    // ---------------------------------------------------------------- the dialog itself

    public boolean isDisplayed() {
        return isPresent(elements.getDialogLocator());
    }

    public String getHeading() {
        return getText(elements.getHeading()).trim();
    }

    public String getDescription() {
        return getText(elements.getDescription()).trim();
    }

    public String getRole() {
        return getAttribute(elements.getDialog(), "role");
    }

    public boolean isModal() {
        return "true".equals(getAttribute(elements.getDialog(), "aria-modal"));
    }

    // ---------------------------------------------------------------- loading

    /**
     * The loading paragraph's text, or an empty string when it is not on screen. The list arrives from the server a
     * moment after the dialog frame, so the paragraph may already be gone; it is read in one step so it cannot vanish
     * between a presence check and a read.
     */
    public String getLoadingTextIfShown() {
        List<WebElement> shown = getDriver().findElements(elements.getLoadingLocator());
        if (shown.isEmpty()) {
            reportStep("The loading text is not on screen", "INFO");
            return "";
        }
        try {
            return getText(shown.get(0)).trim();
        } catch (RuntimeException e) {
            return "";
        }
    }

    public void waitForLoadingToFinish() {
        waitForElementInvisible(elements.getLoadingLocator());
    }

    // ---------------------------------------------------------------- one cron (read only)

    public boolean isCronListed(String cronName) {
        return isPresent(By.xpath(TenantCronsElements.rowLocatorXpath(cronName)));
    }

    public String getCronStatus(String cronName) {
        return getText(locateElement("xpath", TenantCronsElements.statusXpath(cronName))).trim();
    }

    /** Whether the cron's Remove button is on screen. It is never clicked. */
    public boolean isRemoveDisplayed(String cronName) {
        By remove = By.xpath(TenantCronsElements.removeButtonXpath(cronName));
        return isPresent(remove) && isDisplayed(locateElement("xpath", TenantCronsElements.removeButtonXpath(cronName)));
    }

    /** Whether the cron's Remove button is enabled. It is never clicked. */
    public boolean isRemoveEnabled(String cronName) {
        return isEnabled(locateElement("xpath", TenantCronsElements.removeButtonXpath(cronName)));
    }

    public String getRemoveButtonText(String cronName) {
        return getText(locateElement("xpath", TenantCronsElements.removeButtonXpath(cronName))).trim();
    }

    // ---------------------------------------------------------------- closing attempts that the portal ignores

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
     * Clicks outside the dialog. MUI handles that click on its full-viewport container (the element that surrounds the
     * dialog), not on the visual backdrop, so the container is the target: a real mouse click near its corner, which
     * lies outside the dialog, is what a user's click on the backdrop reaches. The point is derived from the
     * container's own size, not hard-coded.
     */
    public void clickBackdrop() {
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

    // ---------------------------------------------------------------- controls the dialog does not have

    public boolean hasSearchControl() {
        return isPresent(elements.getSearchLocator());
    }

    public boolean hasFilterControl() {
        return isPresent(elements.getFilterLocator());
    }

    public boolean hasPagination() {
        return isPresent(elements.getPaginationLocator());
    }

    public boolean hasTabs() {
        return isPresent(elements.getTabsLocator());
    }
}
