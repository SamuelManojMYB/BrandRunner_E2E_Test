package ai.metayb.ui.components.budget;

/**
 * Locators for the Budget module of the main QA app (/anp/budget), taken from the live DOM of the Yeppie business
 * unit. The app is MUI with hashed emotion classes and no data-testid, and its inputs have no name or id, so locators
 * use what is stable: link hrefs, button text, placeholders, the captions printed above fields, MUI role attributes
 * and the {@code aria-label} MUI puts on tooltip wrappers ("Edit this activity", "Remove activity", ...).
 *
 * Product rows hold 3 inputs each (the Select's hidden input, planned quantity for sampling, planned quantity for
 * sales) and Manpower rows hold 6 (the Select's hidden input, unit, rate, standard rate, days, line total), so row
 * fields are addressed by input position inside their section; that is the DOM order checked live.
 */
public final class BudgetElements {

    private BudgetElements() {
    }

    /** An XPath string literal that is safe for any quote characters. */
    public static String lit(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        StringBuilder sb = new StringBuilder("concat(");
        String[] parts = value.split("'", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                sb.append(", \"'\", ");
            }
            sb.append("'").append(parts[i]).append("'");
        }
        return sb.append(")").toString();
    }

    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";

    /**
     * A leaf element whose text equals {@code caption} ignoring case. Captions such as "Country *" are styled
     * upper-case by CSS, so what a user reads ("COUNTRY *") differs from the DOM text; matching case-insensitively
     * keeps the locator independent of that styling.
     */
    public static String caption(String caption) {
        return "*[not(*)][not(self::script or self::style or self::noscript)][translate(normalize-space(),'" + UPPER + "','" + LOWER + "')=" + lit(caption.toLowerCase()) + "]";
    }

    /** A leaf element whose text contains {@code part} ignoring case. */
    public static String captionContaining(String part) {
        return "*[not(*)][not(self::script or self::style or self::noscript)][contains(translate(normalize-space(),'" + UPPER + "','" + LOWER + "')," + lit(part.toLowerCase()) + ")]";
    }

    /** The first amount (a leaf containing the naira sign) after the leaf whose text equals {@code label}. */
    public static String amountAfter(String label) {
        return "//*[not(*)][not(self::script or self::style or self::noscript)][normalize-space()=" + lit(label) + "]/following::*[not(*)][not(self::script or self::style or self::noscript)][contains(normalize-space(),'₦')][1]";
    }

    // ------------------------------------------------------------------------------------------------ side menu
    public static final class Nav {
        private Nav() {
        }

        /** The business unit name in the side-menu header: the closest paragraph before the Dashboard link. */
        public static final String CURRENT_BU = "//a[@href='/anp/dashboard']/preceding::p[normalize-space()][1]";
        public static final String MENU_BUDGET = "//a[@href='/anp/budget']";
        public static final String MENU_DASHBOARD = "//a[@href='/anp/dashboard']";

        /** A row of the open business-unit dropdown (a Paper holding one paragraph per business unit). */
        public static String buOption(String name) {
            return "//div[contains(@class,'MuiPaper-root')]//p[normalize-space()=" + lit(name) + "]";
        }
    }

    // ------------------------------------------------------------------------------------------------ budget list
    public static final class ListPage {
        private ListPage() {
        }

        public static final String LIST_TOGGLE = "//div[normalize-space()='List'][not(.//div)]";
        public static final String TABLE_TOGGLE = "//div[normalize-space()='Table'][not(.//div)]";
        public static final String SEARCH = "//input[@placeholder='Search...']";
        public static final String EXPORT = "//button[normalize-space()='Export']";
        public static final String CREATE_BUDGET = "//button[normalize-space()='Create Budget']";
        public static final String FILTERS_HEADING = "//p[normalize-space()='Filters']";
        public static final String RESET_FILTERS = "//button[normalize-space()='Reset Filters']";
        public static final String APPLY_FILTERS = "//button[normalize-space()='Apply Filters']";
        /** One button that reads "Expand All" and, once expanded, "Collapse All". */
        public static final String EXPAND_TOGGLE = "//button[normalize-space()='Expand All' or normalize-space()='Collapse All']";
        public static final String EXPAND_ALL = "//button[normalize-space()='Expand All']";
        public static final String COLLAPSE_ALL = "//button[normalize-space()='Collapse All']";
        public static final String CURRENCY_SYMBOL = "//*[not(*)][not(self::script or self::style or self::noscript)][contains(normalize-space(),'₦')]";
        public static String columnHeader(String caption) {
            return "//th[normalize-space()=" + lit(caption) + "]";
        }
        public static final String HEADING = "//h6[normalize-space()='Standard Activity']";
        /** "(636 Total)" next to the heading. */
        public static final String TOTAL = "//h6[normalize-space()='Standard Activity']/following::*[not(*)][not(self::script or self::style or self::noscript)][contains(normalize-space(),'Total)')][1]";
        public static final String MONTH_FILTER_INPUT = "//label[normalize-space()='Month Year']/following::input[1]";
        public static final String ROWS = "//tr[contains(@class,'MuiTableRow-hover')]";

        /** The filter caption (Country, State, County, City, Activity, Status) as a label. */
        public static String filterLabel(String caption) {
            return "//label[normalize-space()=" + lit(caption) + "]";
        }

        /** The MUI multi-Select belonging to a filter caption, e.g. "All Country (9)". */
        public static String filterSelect(String caption) {
            return filterLabel(caption) + "/parent::*//*[@role='button' or @role='combobox']";
        }

        public static String group(String name) {
            return "//*[self::p or self::span or self::h6][normalize-space()=" + lit(name) + "]";
        }

        public static String rowContaining(String text) {
            return ROWS + "[.//*[contains(normalize-space(), " + lit(text) + ")]]";
        }

        public static String rowViewButton(String rowText) {
            return rowContaining(rowText) + "//button[@aria-label='View']";
        }

        public static String rowEditButton(String rowText) {
            return rowContaining(rowText) + "//button[normalize-space()='Edit' or @aria-label='Edit']";
        }

        public static String rowDuplicateButton(String rowText) {
            return rowContaining(rowText) + "//button[@aria-label='Duplicate']";
        }

        public static final String NEXT_PAGE = "//button[@aria-label='Go to next page']";
        public static final String PREVIOUS_PAGE = "//button[@aria-label='Go to previous page']";
    }

    // ------------------------------------------------------------------------------------------------ create / edit page
    public static final class Create {
        private Create() {
        }

        public static final String HEADING = "//h6[normalize-space()='Create Budget']";
        public static final String COUNTER_CHIP = "(//*[not(*)][not(self::script or self::style or self::noscript)][contains(normalize-space(),'/25')])[1]";
        public static final String HELPER_TEXT = "//*[not(*)][not(self::script or self::style or self::noscript)][contains(normalize-space(),'Max 25 activities')]";
        public static final String STEP1_HEADING = "//h6[starts-with(normalize-space(),'Step 1')]";
        public static final String ACTIVITIES_HEADING = "//h6[normalize-space()='Standard Activities']";
        public static final String COUNTRY_SELECT = "//" + caption("Country *") + "/following::div[@role='combobox'][1]";
        /** A plain text input while no Country is chosen, an input[type=month] afterwards; found from its caption in both states. */
        public static final String MONTH_INPUT = "//" + caption("Budget Month *") + "/following::input[1]";
        public static final String COUNTRY_CAPTION = "//" + caption("Country *");
        public static final String ADD_ACTIVITY = "//button[normalize-space()='Add Activity']";
        public static final String CANCEL = "//button[normalize-space()='Cancel']";
        public static final String SAVE_AS_DRAFT = "//button[starts-with(normalize-space(),'Save as Draft')]";
        public static final String SUBMIT = "//button[starts-with(normalize-space(),'Submit')]";
        public static final String UNCHECK_ALL = "//*[@aria-label='Uncheck all']";
        /** "Total Requested:" in the page header, "Total:" beside Standard Activities, "Grand Total:" in the footer. */
        public static final String HEADER_TOTAL_REQUESTED = amountAfter("Total Requested:");
        public static final String ACTIVITIES_TOTAL = amountAfter("Total:");
        public static final String GRAND_TOTAL = amountAfter("Grand Total:");
        public static final String READY_FOOTER = "//*[not(*)][not(self::script or self::style or self::noscript)][contains(normalize-space(),' of ')][contains(normalize-space(),'ready')]";
        public static final String SELECTED_FOOTER = "//*[not(*)][not(self::script or self::style or self::noscript)][contains(normalize-space(),' of ')][contains(normalize-space(),'selected')]";
        public static final String ADDED_TEXT = "//*[not(*)][not(self::script or self::style or self::noscript)][contains(normalize-space(),'added)')]";
        public static final String ANY_DIALOG = "//div[@role='dialog']";
        /** Anything that can present a warning or confirmation: an alert, a snackbar/toast, or a dialog. */
        public static final String WARNING_SURFACE = "//*[@role='alert' or @role='alertdialog' or contains(@class,'MuiSnackbar-root') or contains(@class,'MuiAlert-root')] | //div[@role='dialog']";
        /** A dialog that is neither the activity form nor the activity picker (a confirmation such as "Discard changes?"). */
        public static final String CONFIRM_DIALOG = "//div[@role='dialog'][not(.//button[normalize-space()='Done' or normalize-space()='Update & Done'])][not(.//h2[contains(normalize-space(),'Add Standard Activity for Budget')])] | //*[@role='alertdialog']";
        public static final String OK_BUTTON = "//button[normalize-space()='OK']";
        public static final String KEEP_IT_BUTTON = "//button[normalize-space()='Keep it']";
        /** Buttons that confirm a submission dialog, whichever wording the app uses (never the delete buttons). */
        public static final String SUBMIT_CONFIRM_BUTTON = "//div[@role='dialog' or @role='alertdialog']//button[normalize-space()='Yes' or normalize-space()='Confirm' or normalize-space()='Submit' or normalize-space()='OK' or normalize-space()='Yes, submit']";
        public static final String LIMIT_REACHED = "//*[not(*)][not(self::script or self::style or self::noscript)][contains(normalize-space(),'Limit reached')]";

        public static String text(String contains) {
            return "//*[not(*)][not(self::script or self::style or self::noscript)][contains(normalize-space(), " + lit(contains) + ")]";
        }

        public static final String CARDS = "//div[contains(@class,'MuiPaper-outlined')][.//*[@aria-label='Expand to view']]";

        public static String card(int oneBased) {
            return "(" + CARDS + ")[" + oneBased + "]";
        }

        public static String cardCheckbox(int n) {
            return card(n) + "//input[@type='checkbox']";
        }

        public static String cardEdit(int n) {
            return card(n) + "//button[normalize-space()='Edit']";
        }

        public static String cardView(int n) {
            return card(n) + "//button[normalize-space()='View']";
        }

        public static String cardRemove(int n) {
            return card(n) + "//*[@aria-label='Remove activity' or starts-with(@aria-label,'Delete activity')]//button";
        }

        public static String cardText(int n) {
            return card(n);
        }
    }

    // ------------------------------------------------------------------------------------------------ add-activity picker
    public static final class Picker {
        private Picker() {
        }

        public static final String DIALOG = "//div[@role='dialog'][.//h2[contains(normalize-space(),'Add Standard Activity for Budget')]]";
        public static final String SEARCH = DIALOG + "//input[@placeholder='Search...']";
        public static final String NO_RESULTS = DIALOG + "//*[not(*)][not(self::script or self::style or self::noscript)][contains(normalize-space(),'No activities found')]";

        public static String addButton(String activity) {
            return DIALOG + "//p[normalize-space()=" + lit(activity) + "]/parent::div//button[normalize-space()='Add']";
        }

        public static final String ROW_NAMES = DIALOG + "//button[normalize-space()='Add']/parent::div/p";
    }

    // ------------------------------------------------------------------------------------------------ activity drawer
    /** The activity form: the only dialog that has a Done (or "Update & Done") button. */
    public static final class Drawer {
        private Drawer() {
        }

        public static final String ROOT = "//div[@role='dialog'][.//button[normalize-space()='Done' or normalize-space()='Update & Done']]";
        public static final String TITLE_INPUT = ROOT + "//input[@placeholder='Enter Budget Title']";
        public static final String HEADING = ROOT + "//h2";
        /** The activity name: the first element with text inside the title (the title also holds the title input, Reset and a hint). */
        public static final String HEADING_NAME = ROOT + "//h2//*[not(*)][not(self::script or self::style or self::noscript)][normalize-space()][1]";
        public static final String RESET = ROOT + "//button[@aria-label='Reset fields']";
        public static final String CLOSE = "(" + ROOT + "//button[normalize-space()='Close'])[last()]";
        public static final String DONE = ROOT + "//button[normalize-space()='Done' or normalize-space()='Update & Done']";
        public static final String DAYS_INPUT = ROOT + "//input[@type='number']";
        public static final String BUDGET_MONTH_FIELD = ROOT + "//" + caption("Budget Month *") + "/following::input[1]";
        /** The "locked" badge next to the Country caption (the Country is fixed from Step 1). */
        public static final String LOCKED_BADGE = ROOT + "//*[not(*)][not(self::script or self::style or self::noscript)][contains(translate(normalize-space(),'" + UPPER + "','" + LOWER + "'),'locked')]";
        public static final String LOCATION_UNLOCK_HINT = ROOT + "//*[not(*)][not(self::script or self::style or self::noscript)][contains(normalize-space(),'Select the last location level above to unlock the form')]";

        /** STATE / COUNTY / CITY Select, found from its caption. */
        public static String locationSelect(String caption) {
            return ROOT + "//" + BudgetElements.caption(caption) + "/following::div[@role='combobox'][1]";
        }

        public static String text(String contains) {
            return ROOT + "//*[not(*)][not(self::script or self::style or self::noscript)][contains(normalize-space(), " + lit(contains) + ")]";
        }

        public static final String HEADER_REQUESTED = ROOT + amountAfter("Requested:");
        public static final String FOOTER_REQUESTED = ROOT + amountAfter("Requested Amount:");
        public static final String WARNINGS = ROOT + "//*[not(*)][not(self::script or self::style or self::noscript)][contains(normalize-space(),'⚠')]";
        public static final String MANPOWER_TOTAL = ROOT + "//" + captionContaining("manpower total") + "/following::*[not(*)][not(self::script or self::style or self::noscript)][contains(normalize-space(),'₦')][1]";

        // Sections: the smallest block holding the section caption and its "Add ..." button.
        // "Product" is printed twice (section title and column header), so the first match is pinned.
        public static final String PRODUCT_SECTION = "(" + ROOT + "//" + caption("Product") + "/ancestor::div[.//button[normalize-space()='Add Product']][1])[1]";
        public static final String MANPOWER_SECTION = "(" + ROOT + "//" + caption("Manpower") + "/ancestor::div[.//button[normalize-space()='Add Manpower']][1])[1]";
        public static final String ADD_PRODUCT = ROOT + "//button[normalize-space()='Add Product']";
        public static final String ADD_MANPOWER = ROOT + "//button[normalize-space()='Add Manpower']";

        public static String productSelect(int row) {
            return "(" + PRODUCT_SECTION + "//div[@role='combobox'])[" + row + "]";
        }

        public static String productInput(int row, int column) {
            // column: 1 = hidden select input, 2 = planned quantity for sampling, 3 = planned quantity for sales
            return "(" + PRODUCT_SECTION + "//input)[" + (3 * (row - 1) + column) + "]";
        }

        public static String manpowerSelect(int row) {
            return "(" + MANPOWER_SECTION + "//div[@role='combobox'])[" + row + "]";
        }

        public static String manpowerInput(int row, int column) {
            // column: 1 hidden select input, 2 unit, 3 rate, 4 standard rate (read-only), 5 days, 6 line total (read-only)
            return "(" + MANPOWER_SECTION + "//input)[" + (6 * (row - 1) + column) + "]";
        }

        public static final String PRODUCT_ROW_COUNT_INPUTS = PRODUCT_SECTION + "//input";
        public static final String MANPOWER_ROW_COUNT_INPUTS = MANPOWER_SECTION + "//input";
        public static final String PRODUCT_ROW_DELETE = PRODUCT_SECTION + "//button[not(normalize-space())][not(@aria-label)]";
        public static final String MANPOWER_ROW_DELETE = MANPOWER_SECTION + "//button[not(normalize-space())][not(@aria-label)]";
    }

    // ------------------------------------------------------------------------------------------------ select popups
    public static final class Popup {
        private Popup() {
        }

        public static final String LISTBOX = "//ul[@role='listbox']";
        public static final String OPTIONS = LISTBOX + "//li[@role='option']";

        public static String option(String text) {
            return OPTIONS + "[normalize-space()=" + lit(text) + "]";
        }

        public static String optionStartingWith(String text) {
            return OPTIONS + "[starts-with(normalize-space(), " + lit(text) + ")]";
        }
    }
}
