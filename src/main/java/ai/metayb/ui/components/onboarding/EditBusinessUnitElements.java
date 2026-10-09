package ai.metayb.ui.components.onboarding;

import org.openqa.selenium.By;

/**
 * Locators for the Edit Business Unit Settings dialog (Dashboard card menu, Edit Details). It is the same eight-section
 * form as Create Business Unit but its title is "Edit Business Unit Settings", so the Create locators (which are scoped to
 * the Create title) cannot be reused; the control structure inside each section is the same, and the same
 * label/name conventions are used here.
 *
 * Everything here is for reading. There is deliberately no locator for the Update button's click target: the loading state
 * is detected through the button's caption only ("Saving..." while settings load, "Update" once they are ready), and the
 * dialog is left through Cancel.
 */
public final class EditBusinessUnitElements {

    public static final String TITLE_PREFIX = "Edit Business Unit Settings";
    public static final String UNSAVED_CHANGES_CHIP_TEXT = "Unsaved changes";

    private static final String FORM_CONTROL = "/ancestor::div[contains(@class,'MuiFormControl-root')][1]";
    private static final String AUTOCOMPLETE_ROOT = "/ancestor::div[contains(@class,'MuiAutocomplete-root')][1]";

    public static final String DIALOG_XPATH =
            "//div[@role='dialog'][.//h2[starts-with(normalize-space(),'" + TITLE_PREFIX + "')]]";

    private EditBusinessUnitElements() {
    }

    // ---- dialog frame ----
    public static By dialog() {
        return By.xpath(DIALOG_XPATH);
    }

    public static By title() {
        return By.xpath(DIALOG_XPATH + "//h2");
    }

    public static By progressIndicators() {
        return By.xpath(DIALOG_XPATH + "//*[@role='progressbar']");
    }

    /** Present only once the settings have loaded; while loading the same button reads "Saving...". Never clicked. */
    public static By updateButtonReady() {
        return By.xpath(DIALOG_XPATH + "//button[normalize-space()='Update']");
    }

    public static By cancelButton() {
        return By.xpath(DIALOG_XPATH + "//button[normalize-space()='Cancel']");
    }

    // ---- sections ----
    private static String panel(String section) {
        return DIALOG_XPATH + "//h6[normalize-space()=" + DashboardPageElements.xpathLiteral(section) + "]/parent::div";
    }

    public static By sectionHeading(String section) {
        return By.xpath(DIALOG_XPATH + "//h6[normalize-space()=" + DashboardPageElements.xpathLiteral(section) + "]");
    }

    // ---- Business Unit Details ----
    public static By name() {
        return By.xpath(panel("Business Unit Details") + "//input[@name='name']");
    }

    /** The Status select's hidden input of the Business Unit Details section; it holds "active" / "inactive". */
    public static By businessUnitStatusValue() {
        return By.xpath(panel("Business Unit Details") + "//label[normalize-space()='Status']" + FORM_CONTROL
                + "//input[@aria-hidden='true']");
    }

    // ---- Currency (Autocomplete) ----
    public static By currencyInput() {
        return By.xpath(panel("Currency") + "//label[starts-with(normalize-space(),'Currency')]" + AUTOCOMPLETE_ROOT
                + "//input[@role='combobox']");
    }

    // ---- integration sections ----
    /** The hidden input of a "<x> Config Status" select, holding "active" / "inactive". */
    public static By configStatusValue(String section, String statusLabel) {
        return By.xpath(panel(section) + "//label[normalize-space()=" + DashboardPageElements.xpathLiteral(statusLabel) + "]"
                + FORM_CONTROL + "//input[@aria-hidden='true']");
    }

    public static By autocompleteInput(String section, String labelStart) {
        return By.xpath(panel(section) + "//label[starts-with(normalize-space()," + DashboardPageElements.xpathLiteral(labelStart)
                + ")]" + AUTOCOMPLETE_ROOT + "//input[@role='combobox']");
    }

    public static By namedInput(String section, String inputName) {
        return By.xpath(panel(section) + "//input[@name=" + DashboardPageElements.xpathLiteral(inputName) + "]");
    }

    // ---- OTP Verification ----
    /** The (invisible, nameless) switch of one OTP flow, found from the flow's exact title. */
    public static By otpSwitch(OtpFlow flow) {
        return By.xpath(panel("OTP Verification") + "//p[normalize-space()=" + DashboardPageElements.xpathLiteral(flow.title())
                + "]/parent::div/parent::div//input[@role='switch']");
    }
}
