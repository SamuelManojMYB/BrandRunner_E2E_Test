package ai.metayb.ui.components.onboarding;

import lombok.Getter;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Locators for the Onboarding Portal tenant dashboard (/dashboard), taken from the live DOM.
 *
 * The dashboard exposes no data-testid, id, name or aria-label of its own, so every locator is a
 * visible-text + structural-relationship XPath (a label {@code <p>} followed by a value {@code <div>},
 * a button next to a known button, {@code role} on dialogs/menus where the app does provide it). The
 * only class names used are MUI's static component classes (MuiCard-root, MuiAvatar-root, ...), never
 * the generated css-* classes. Each string is declared once and shared by its @FindBy field and By twin.
 */
@Getter
public class DashboardPageElements {

    private static final String NOT_IN_DIALOG = "[not(ancestor::*[@role='dialog'])]";

    // ---- header / tenant card ----
    private static final String TENANT_NAME_XPATH = "//div[contains(@class,'MuiCard-root')]//h6" + NOT_IN_DIALOG;
    private static final String PLAN_NAME_XPATH = "//h6" + NOT_IN_DIALOG + "/following-sibling::div//p";
    private static final String USER_NAME_XPATH = "//p[normalize-space()='User Name']/following-sibling::div[1]//p";
    private static final String MAIL_ID_XPATH = "//p[normalize-space()='Mail ID']/following-sibling::div[1]//p";
    private static final String HEADQUARTER_XPATH = "//p[normalize-space()='Headquarter']/following-sibling::div[1]//p";
    private static final String INDUSTRY_XPATH = "//p[normalize-space()='Industry']/following-sibling::div[1]//p";
    // "Business Units" labels both the summary counts and the card list below, so the counts are
    // anchored on the Active/Inactive captions that follow the number.
    private static final String ACTIVE_COUNT_XPATH =
            "//p[normalize-space()='Business Units']/following-sibling::div[1]//span[normalize-space()='Active']/preceding-sibling::span[1]";
    private static final String INACTIVE_COUNT_XPATH =
            "//p[normalize-space()='Business Units']/following-sibling::div[1]//span[normalize-space()='Inactive']/preceding-sibling::span[1]";
    private static final String APPLICATION_URL_LINK_XPATH =
            "//p[normalize-space()='Application URL']/following-sibling::div[1]//a";
    private static final String COPY_URL_BUTTON_XPATH = APPLICATION_URL_LINK_XPATH + "/following-sibling::button";

    // ---- action buttons ----
    private static final String SELECTED_PLAN_BUTTON_XPATH = "//button[normalize-space()='Selected Plan']";
    private static final String TENANT_CRONS_BUTTON_XPATH = "//button[normalize-space()='Tenant Crons']";
    private static final String ADD_BU_BUTTON_XPATH = "//button[normalize-space()='Add Business Unit']";
    // The icon-only three-dot button sits right after Add Business Unit.
    private static final String DASHBOARD_MENU_BUTTON_XPATH = ADD_BU_BUTTON_XPATH + "/following-sibling::button";
    private static final String PROFILE_BUTTON_XPATH = "//button[.//div[contains(@class,'MuiAvatar-root')]]";

    // ---- menus (MUI menus render in a portal with role=menu / role=menuitem) ----
    private static final String MENU_XPATH = "//ul[@role='menu']";
    private static final String MENU_ITEMS_XPATH = MENU_XPATH + "//li[@role='menuitem']";
    private static final String EDIT_SETTINGS_ITEM_XPATH = MENU_ITEMS_XPATH + "[normalize-space()='Edit Settings']";
    // The profile menu has no <li>/menuitem role: its entries are direct-child <div> rows of the <ul>, each
    // holding an icon and a <p> caption. Presence is checked on the caption; clicks go to the whole row, which
    // is far larger than the text and so is not missed while the menu is still opening.
    private static final String CHANGE_PASSWORD_XPATH = MENU_XPATH + "//p[normalize-space()='Change Password']";
    private static final String LOGOUT_XPATH = MENU_XPATH + "//p[normalize-space()='Logout']";
    private static final String LOGOUT_ROW_XPATH = MENU_XPATH + "/div[.//p[normalize-space()='Logout']]";

    // ---- Business Unit cards ----
    private static final String CARD_XPATH =
            "//div[contains(@class,'MuiCard-root')][.//span[starts-with(normalize-space(),'Created on')]]";
    private static final String CARD_NAMES_XPATH = CARD_XPATH + "//p";

    // ---- Selected Plan page (route /dashboard/client/upgrade-plan) ----
    private static final String PLAN_PAGE_HEADING_XPATH = "//p[normalize-space()='Selected Plan']";
    private static final String PLAN_PAGE_BACK_XPATH = "//p[normalize-space()='Back']";

    // ---- dialogs (the app sets role=dialog; they are identified by their <h2> title) ----
    private static final String CRONS_DIALOG_XPATH = "//div[@role='dialog'][.//h2[normalize-space()='Tenant Crons']]";
    // The dialog frame renders first; its cron list is filled in afterwards, so entries are waited for explicitly.
    private static final String CRON_NAMES_XPATH = CRONS_DIALOG_XPATH + "//li//p";
    private static final String CRON_STATUSES_XPATH =
            CRONS_DIALOG_XPATH + "//li//div[contains(@class,'MuiChip-root')]//span";
    private static final String CRONS_CLOSE_XPATH = CRONS_DIALOG_XPATH + "//button[normalize-space()='Close']";

    public static final String CREATE_DIALOG_XPATH =
            "//div[@role='dialog'][.//h2[starts-with(normalize-space(),'Create Business Unit')]]";
    private static final String CREATE_NAME_INPUT_XPATH = CREATE_DIALOG_XPATH + "//input[@name='name']";
    private static final String CREATE_CANCEL_XPATH = CREATE_DIALOG_XPATH
            + "//button[normalize-space()='Create']/preceding-sibling::button[normalize-space()='Cancel']";

    private static final String SETTINGS_DIALOG_XPATH =
            "//div[@role='dialog'][.//h2[normalize-space()='Organisational Settings']]";
    // The dialog's inner accordion panels have their own Cancel buttons; the footer Cancel is the one
    // that immediately precedes Update.
    private static final String SETTINGS_CANCEL_XPATH = SETTINGS_DIALOG_XPATH
            + "//button[normalize-space()='Update']/preceding-sibling::button[normalize-space()='Cancel']";

    private static final String SPINNER_XPATH = "//span[@role='progressbar']";
    private static final String LOGIN_PASSWORD_XPATH = "//input[@name='password']";

    @FindBy(xpath = TENANT_NAME_XPATH)
    private WebElement tenantName;
    private final By tenantNameLocator = By.xpath(TENANT_NAME_XPATH);

    @FindBy(xpath = PLAN_NAME_XPATH)
    private WebElement planName;

    @FindBy(xpath = USER_NAME_XPATH)
    private WebElement userName;

    @FindBy(xpath = MAIL_ID_XPATH)
    private WebElement mailId;

    @FindBy(xpath = HEADQUARTER_XPATH)
    private WebElement headquarter;

    @FindBy(xpath = INDUSTRY_XPATH)
    private WebElement industry;

    @FindBy(xpath = ACTIVE_COUNT_XPATH)
    private WebElement activeCount;

    @FindBy(xpath = INACTIVE_COUNT_XPATH)
    private WebElement inactiveCount;

    @FindBy(xpath = APPLICATION_URL_LINK_XPATH)
    private WebElement applicationUrlLink;

    @FindBy(xpath = COPY_URL_BUTTON_XPATH)
    private WebElement copyUrlButton;

    @FindBy(xpath = SELECTED_PLAN_BUTTON_XPATH)
    private WebElement selectedPlanButton;

    @FindBy(xpath = TENANT_CRONS_BUTTON_XPATH)
    private WebElement tenantCronsButton;

    @FindBy(xpath = ADD_BU_BUTTON_XPATH)
    private WebElement addBusinessUnitButton;

    @FindBy(xpath = DASHBOARD_MENU_BUTTON_XPATH)
    private WebElement dashboardMenuButton;

    @FindBy(xpath = PROFILE_BUTTON_XPATH)
    private WebElement profileButton;
    private final By profileButtonLocator = By.xpath(PROFILE_BUTTON_XPATH);

    @FindBy(xpath = MENU_XPATH)
    private WebElement menu;
    private final By menuLocator = By.xpath(MENU_XPATH);

    @FindBy(xpath = MENU_ITEMS_XPATH)
    private List<WebElement> menuItems;

    @FindBy(xpath = EDIT_SETTINGS_ITEM_XPATH)
    private WebElement editSettingsItem;

    private final By changePasswordLocator = By.xpath(CHANGE_PASSWORD_XPATH);

    private final By logoutLocator = By.xpath(LOGOUT_XPATH);
    @FindBy(xpath = LOGOUT_ROW_XPATH)
    private WebElement logoutRow;
    private final By logoutRowLocator = By.xpath(LOGOUT_ROW_XPATH);

    @FindBy(xpath = CARD_NAMES_XPATH)
    private List<WebElement> businessUnitNames;

    @FindBy(xpath = PLAN_PAGE_BACK_XPATH)
    private WebElement planPageBack;
    private final By planPageHeadingLocator = By.xpath(PLAN_PAGE_HEADING_XPATH);

    private final By tenantCronsDialogLocator = By.xpath(CRONS_DIALOG_XPATH);
    @FindBy(xpath = CRON_NAMES_XPATH)
    private List<WebElement> cronNames;
    private final By cronNameLocator = By.xpath(CRON_NAMES_XPATH);
    @FindBy(xpath = CRON_STATUSES_XPATH)
    private List<WebElement> cronStatuses;
    private final By cronStatusLocator = By.xpath(CRON_STATUSES_XPATH);
    @FindBy(xpath = CRONS_CLOSE_XPATH)
    private WebElement cronsCloseButton;

    private final By createDialogLocator = By.xpath(CREATE_DIALOG_XPATH);
    private final By createNameInputLocator = By.xpath(CREATE_NAME_INPUT_XPATH);
    @FindBy(xpath = CREATE_CANCEL_XPATH)
    private WebElement createCancelButton;

    private final By settingsDialogLocator = By.xpath(SETTINGS_DIALOG_XPATH);
    @FindBy(xpath = SETTINGS_CANCEL_XPATH)
    private WebElement settingsCancelButton;

    private final By spinnerLocator = By.xpath(SPINNER_XPATH);
    private final By loginPasswordLocator = By.xpath(LOGIN_PASSWORD_XPATH);
    // Either the login form (unauthenticated redirect) or the signed-in header: whichever renders first.
    private final By loginOrDashboardLocator = By.xpath(LOGIN_PASSWORD_XPATH + " | " + PROFILE_BUTTON_XPATH);

    // ---- locators that depend on a name/label ----

    public static By createSectionLocator(String section) {
        return By.xpath(CREATE_DIALOG_XPATH + "//h6[normalize-space()=" + xpathLiteral(section) + "]");
    }

    // Left-nav captions of the settings dialog are <span>s; their panel headings are h6 with different text.
    public static By settingsSectionLocator(String section) {
        return By.xpath(SETTINGS_DIALOG_XPATH + "//span[normalize-space(text())=" + xpathLiteral(section) + "]");
    }

    public static String businessUnitCardXpath(String name) {
        return "//p[normalize-space()=" + xpathLiteral(name) + "]/ancestor::div[contains(@class,'MuiCard-root')][1]";
    }

    public static String businessUnitStatusXpath(String name) {
        return businessUnitCardXpath(name) + "//span[normalize-space()='Active' or normalize-space()='Inactive']";
    }

    public static String businessUnitCreatedXpath(String name) {
        return businessUnitCardXpath(name) + "//span[starts-with(normalize-space(),'Created on')]";
    }

    public static String businessUnitMenuButtonXpath(String name) {
        return businessUnitCardXpath(name) + "//button";
    }

    public static String xpathLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        return "concat('" + value.replace("'", "', \"'\", '") + "')";
    }
}
