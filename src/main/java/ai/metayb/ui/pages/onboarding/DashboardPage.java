package ai.metayb.ui.pages.onboarding;

import ai.metayb.config.ConfigManager;
import ai.metayb.ui.components.onboarding.DashboardPageElements;
import ai.metayb.ui.wrappers.BaseDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import java.util.ArrayList;
import java.util.List;

public class DashboardPage extends BaseDriver {

    private static final String DASHBOARD_PATH = "/dashboard";
    private static final String PLAN_PAGE_PATH = "/upgrade-plan";
    // Dashboard data is fetched after the shell renders, so the first load gets a longer wait than the 20s default.
    private static final int LOAD_TIMEOUT_SECONDS = 30;

    private final DashboardPageElements elements;

    public DashboardPage() {
        super();
        elements = new DashboardPageElements();
        PageFactory.initElements(getDriver(), elements);
    }

    // ---------------------------------------------------------------- opening / loading

    /** Navigates to the dashboard route; an unauthenticated session is redirected to /login by the app. */
    public void open() {
        navigateToUrl(dashboardUrl());
    }

    /** Waits for whichever renders first: the signed-in header or the login form. */
    public void waitForDashboardOrLoginPage() {
        waitForElementVisible(elements.getLoginOrDashboardLocator(), LOAD_TIMEOUT_SECONDS);
    }

    public boolean isLoginPageShown() {
        return isPresent(elements.getLoginPasswordLocator());
    }

    /** Waits for the tenant data to render and the loading spinner to disappear. */
    public void waitForDashboardLoaded() {
        waitForElementVisible(elements.getTenantNameLocator(), LOAD_TIMEOUT_SECONDS);
        waitForElementInvisible(elements.getSpinnerLocator(), LOAD_TIMEOUT_SECONDS);
    }

    /** The dashboard also renders at /dashboard/client, so the path is only required to start with /dashboard. */
    public boolean isDashboardDisplayed() {
        return isPresent(elements.getTenantNameLocator())
                && isPresent(elements.getProfileButtonLocator())
                && getCurrentUrl().contains(DASHBOARD_PATH);
    }

    // ---------------------------------------------------------------- tenant information

    public String getTenantName() {
        return getText(elements.getTenantName()).trim();
    }

    public String getPlanName() {
        return getText(elements.getPlanName()).trim();
    }

    public String getUserName() {
        return getText(elements.getUserName()).trim();
    }

    public String getEmail() {
        return getText(elements.getMailId()).trim();
    }

    public String getHeadquarters() {
        return getText(elements.getHeadquarter()).trim();
    }

    public String getIndustry() {
        return getText(elements.getIndustry()).trim();
    }

    public int getActiveBusinessUnitCount() {
        return parseCount(getText(elements.getActiveCount()), "active Business Unit count");
    }

    public int getInactiveBusinessUnitCount() {
        return parseCount(getText(elements.getInactiveCount()), "inactive Business Unit count");
    }

    /** The text shown for the link (the tenant sub-domain, not the full URL). */
    public String getApplicationUrlText() {
        return getText(elements.getApplicationUrlLink()).trim();
    }

    /** The link's absolute href. */
    public String getApplicationUrlHref() {
        return getAttribute(elements.getApplicationUrlLink(), "href");
    }

    public boolean isCopyApplicationUrlControlEnabled() {
        return isDisplayed(elements.getCopyUrlButton()) && isEnabled(elements.getCopyUrlButton());
    }

    // ---------------------------------------------------------------- Business Units

    public List<String> getBusinessUnitNames() {
        List<String> names = new ArrayList<>();
        for (WebElement name : elements.getBusinessUnitNames()) {
            names.add(getText(name).trim());
        }
        return names;
    }

    public String getBusinessUnitStatus(String businessUnitName) {
        return getText(locateElement("xpath", DashboardPageElements.businessUnitStatusXpath(businessUnitName))).trim();
    }

    /** The full caption, e.g. "Created on dd/mm/yyyy". */
    public String getBusinessUnitCreatedText(String businessUnitName) {
        return getText(locateElement("xpath", DashboardPageElements.businessUnitCreatedXpath(businessUnitName))).trim();
    }

    public void openBusinessUnitMenu(String businessUnitName) {
        click(locateElement("xpath", DashboardPageElements.businessUnitMenuButtonXpath(businessUnitName)));
        waitForElementVisible(elements.getMenuLocator());
    }

    /** Entries of whichever dashboard/Business Unit menu is currently open. */
    public List<String> getOpenMenuOptions() {
        List<String> options = new ArrayList<>();
        for (WebElement item : elements.getMenuItems()) {
            options.add(getText(item).trim());
        }
        return options;
    }

    /** Closes the open menu with Escape (the wrapper has no key-press method, so it is sent as text). */
    public void closeOpenMenu() {
        typeWithoutClear(elements.getMenu(), Keys.ESCAPE.toString());
        waitForElementInvisible(elements.getMenuLocator());
    }

    // ---------------------------------------------------------------- Selected Plan

    public void clickSelectedPlan() {
        click(elements.getSelectedPlanButton());
        waitForElementVisible(elements.getPlanPageHeadingLocator());
    }

    public boolean isSelectedPlanPageDisplayed() {
        return getCurrentUrl().contains(PLAN_PAGE_PATH) && isPresent(elements.getPlanPageHeadingLocator());
    }

    /** Uses the page's own Back control, which returns to the dashboard. */
    public void returnFromSelectedPlan() {
        click(elements.getPlanPageBack());
        waitForDashboardLoaded();
    }

    // ---------------------------------------------------------------- Tenant Crons

    public void openTenantCrons() {
        click(elements.getTenantCronsButton());
        waitForElementVisible(elements.getTenantCronsDialogLocator());
    }

    public boolean isTenantCronsDialogDisplayed() {
        return isPresent(elements.getTenantCronsDialogLocator());
    }

    /** Waits for the dialog's cron list to be populated: the frame appears before its entries do. */
    public void waitForTenantCronEntries() {
        waitForElementVisible(elements.getCronNameLocator());
        waitForElementVisible(elements.getCronStatusLocator());
    }

    public List<String> getTenantCronNames() {
        List<String> names = new ArrayList<>();
        for (WebElement name : elements.getCronNames()) {
            names.add(getText(name).trim());
        }
        return names;
    }

    public List<String> getTenantCronStatuses() {
        List<String> statuses = new ArrayList<>();
        for (WebElement status : elements.getCronStatuses()) {
            statuses.add(getText(status).trim());
        }
        return statuses;
    }

    /** Deliberately no method for the dialog's Remove button: automation must never remove a cron. */
    public void closeTenantCrons() {
        click(elements.getCronsCloseButton());
        waitForElementInvisible(elements.getTenantCronsDialogLocator());
    }

    // ---------------------------------------------------------------- Add Business Unit

    public void clickAddBusinessUnit() {
        click(elements.getAddBusinessUnitButton());
        waitForElementVisible(elements.getCreateDialogLocator());
    }

    public boolean isCreateBusinessUnitDialogDisplayed() {
        return isPresent(elements.getCreateDialogLocator());
    }

    public boolean isCreateBusinessUnitNameFieldDisplayed() {
        return isPresent(elements.getCreateNameInputLocator());
    }

    public boolean isCreateBusinessUnitSectionDisplayed(String section) {
        return isPresent(DashboardPageElements.createSectionLocator(section));
    }

    /** Cancels without creating anything. There is intentionally no method that submits the dialog. */
    public void closeCreateBusinessUnitDialog() {
        click(elements.getCreateCancelButton());
        waitForElementInvisible(elements.getCreateDialogLocator());
    }

    // ---------------------------------------------------------------- dashboard menu / Organisational Settings

    public void openDashboardMenu() {
        click(elements.getDashboardMenuButton());
        waitForElementVisible(elements.getMenuLocator());
    }

    /** Requires the dashboard menu to be open (see {@link #openDashboardMenu()}). */
    public void openOrganisationalSettings() {
        click(elements.getEditSettingsItem());
        waitForElementVisible(elements.getSettingsDialogLocator());
    }

    public boolean isOrganisationalSettingsDialogDisplayed() {
        return isPresent(elements.getSettingsDialogLocator());
    }

    public boolean isOrganisationalSettingsSectionDisplayed(String section) {
        return isPresent(DashboardPageElements.settingsSectionLocator(section));
    }

    /** Cancels without saving. There is intentionally no method that clicks Update. */
    public void closeOrganisationalSettings() {
        click(elements.getSettingsCancelButton());
        waitForElementInvisible(elements.getSettingsDialogLocator());
    }

    // ---------------------------------------------------------------- profile menu

    public void openProfileMenu() {
        click(elements.getProfileButton());
        waitForElementVisible(elements.getMenuLocator());
    }

    public boolean isChangePasswordOptionDisplayed() {
        return isPresent(elements.getChangePasswordLocator());
    }

    public boolean isLogoutOptionDisplayed() {
        return isPresent(elements.getLogoutLocator());
    }

    /** Signs out through the profile menu and waits for the login form. */
    public void logout() {
        openProfileMenu();
        waitForElementClickable(elements.getLogoutRowLocator());
        click(elements.getLogoutRow());
        waitForElementVisible(elements.getLoginPasswordLocator(), LOAD_TIMEOUT_SECONDS);
    }

    // ----------------------------------------------------------------

    private String dashboardUrl() {
        return ConfigManager.getOnboardingUiUrl().replaceAll("/+$", "") + DASHBOARD_PATH;
    }

    private int parseCount(String text, String what) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            reportStep("The " + what + " is not a number: '" + text.trim() + "'", "FAIL");
            return 0;
        }
    }
}
