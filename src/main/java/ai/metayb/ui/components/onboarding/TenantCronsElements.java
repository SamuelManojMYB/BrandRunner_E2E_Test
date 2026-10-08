package ai.metayb.ui.components.onboarding;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Locators for the Tenant Crons dialog, from the live DOM. They add only what {@link DashboardPageElements} does not
 * already offer (dialog attributes, heading, description, loading text, per-cron rows and controls, the pieces needed to
 * click outside the dialog, and the unsupported controls); opening, the cron name/status lists and Close stay with the
 * Dashboard page.
 *
 * The dialog is MUI's: {@code div[role=dialog]} (the paper) inside a full-viewport {@code div[role=presentation]}
 * container. A click outside the paper lands on that container, which is what carries MUI's backdrop-click handling,
 * so it is the element to click for "click the backdrop". A cron row is the {@code li} holding the cron's name
 * paragraph; its status is the chip and its action is a button, found inside that row. Nothing here locates an Add
 * button: no test may add or remove a cron.
 */
@Getter
public class TenantCronsElements {

    /** Same locator as the Dashboard's, which keeps its own copy private. */
    private static final String DIALOG_XPATH = "//div[@role='dialog'][.//h2[normalize-space()='Tenant Crons']]";
    private static final String CONTAINER_XPATH = DIALOG_XPATH + "/parent::div[@role='presentation']";

    private static final String HEADING_XPATH = DIALOG_XPATH + "//h2[normalize-space()='Tenant Crons']";
    private static final String DESCRIPTION_XPATH = DIALOG_XPATH + "//p[normalize-space()='Manage cron integrations at the tenant level.']";
    private static final String LOADING_XPATH = DIALOG_XPATH + "//p[normalize-space()='Loading cron details...']";

    /** Controls the dialog does not have. Close and the cron actions are ordinary buttons and are not matched here. */
    private static final String SEARCH_XPATH = DIALOG_XPATH + "//input | " + DIALOG_XPATH + "//*[@role='searchbox' or @role='textbox' or @role='combobox']";
    private static final String FILTER_XPATH = DIALOG_XPATH + "//select | " + DIALOG_XPATH + "//*[@role='listbox' or @role='menu' or @role='switch']"
            + " | " + DIALOG_XPATH + "//button[@aria-haspopup]";
    private static final String PAGINATION_XPATH = DIALOG_XPATH + "//nav | " + DIALOG_XPATH + "//*[@aria-label='pagination navigation' or contains(@class,'MuiPagination-root') or contains(@class,'MuiTablePagination-root')]";
    private static final String TABS_XPATH = DIALOG_XPATH + "//*[@role='tablist' or @role='tab']";

    @FindBy(xpath = DIALOG_XPATH)
    private WebElement dialog;
    private final By dialogLocator = By.xpath(DIALOG_XPATH);

    @FindBy(xpath = CONTAINER_XPATH)
    private WebElement container;

    @FindBy(xpath = HEADING_XPATH)
    private WebElement heading;

    @FindBy(xpath = DESCRIPTION_XPATH)
    private WebElement description;

    private final By loadingLocator = By.xpath(LOADING_XPATH);

    private final By searchLocator = By.xpath(SEARCH_XPATH);
    private final By filterLocator = By.xpath(FILTER_XPATH);
    private final By paginationLocator = By.xpath(PAGINATION_XPATH);
    private final By tabsLocator = By.xpath(TABS_XPATH);

    @FindBy(xpath = DIALOG_XPATH + "//li")
    private List<WebElement> rows;

    // ---- locators that depend on a cron name ----

    /** The row of a cron, found from the exact text of its name. */
    private static String rowXpath(String cronName) {
        return DIALOG_XPATH + "//li[.//p[normalize-space()=" + DashboardPageElements.xpathLiteral(cronName) + "]]";
    }

    public static String rowLocatorXpath(String cronName) {
        return rowXpath(cronName);
    }

    public static String statusXpath(String cronName) {
        return rowXpath(cronName) + "//div[contains(@class,'MuiChip-root')]//span";
    }

    /** The row's Remove button. Only ever read (displayed, enabled, text): there is no way to click it from the page object. */
    public static String removeButtonXpath(String cronName) {
        return rowXpath(cronName) + "//button[normalize-space()='Remove']";
    }
}
