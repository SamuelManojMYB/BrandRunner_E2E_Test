package ai.metayb.ui.pages.budget;

import ai.metayb.ui.components.budget.BudgetElements.Drawer;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;
import java.util.List;

/**
 * The activity form (a dialog drawer) of Create Budget / Edit Bulk Budget. {@link #clickDone()} is the one method that
 * WRITES: the application saves the activity to the database when Done is pressed (the document calls it a pre-draft).
 * Every other method only reads or edits unsaved form values.
 */
public class BudgetActivityDrawer extends BudgetPageBase {

    // ---------------------------------------------------------------- state

    public boolean isOpen() {
        return visible(Drawer.ROOT);
    }

    public void waitUntilOpen() {
        mustBeVisible(Drawer.ROOT, "The activity form");
    }

    public void waitUntilClosed() {
        if (!waitForGone(Drawer.ROOT, UI_TIMEOUT_SECONDS)) {
            reportStep("The activity form did not close within " + UI_TIMEOUT_SECONDS + "s", "FAIL");
        }
    }

    /** The activity name in the drawer title, e.g. "Event Sampling". */
    public String heading() {
        return domTextOf(Drawer.HEADING_NAME);
    }

    public boolean isTextShown(String contains) {
        return visible(Drawer.text(contains));
    }

    public List<String> warnings() {
        return textsOf(Drawer.WARNINGS);
    }

    // ---------------------------------------------------------------- title and location

    public void enterTitle(String title) {
        replaceText(find(Drawer.TITLE_INPUT), title);
    }

    public void clearTitle() {
        clearField(find(Drawer.TITLE_INPUT));
    }

    public String title() {
        return valueOf(Drawer.TITLE_INPUT);
    }

    public boolean isCountryLocked() {
        return visible(Drawer.LOCKED_BADGE);
    }

    public boolean isFormLocked() {
        return visible(Drawer.LOCATION_UNLOCK_HINT);
    }

    public boolean isLocationSelectEnabled(String caption) {
        List<WebElement> found = getDriver().findElements(xp(Drawer.locationSelect(caption)));
        return !found.isEmpty() && !"true".equals(found.get(0).getDomAttribute("aria-disabled"));
    }

    public String selectedLocation(String caption) {
        return textOf(Drawer.locationSelect(caption));
    }

    public List<String> locationOptions(String caption) {
        return readSelectOptions(Drawer.locationSelect(caption));
    }

    public void selectLocation(String caption, String name) {
        selectOption(Drawer.locationSelect(caption), name);
    }

    /** Chooses State, County and City in order; each choice unlocks the next level. */
    public void selectLocation(String state, String county, String city) {
        selectLocation("State", state);
        waitUntil(() -> isLocationSelectEnabled("County"), UI_TIMEOUT_SECONDS);
        selectLocation("County", county);
        waitUntil(() -> isLocationSelectEnabled("City"), UI_TIMEOUT_SECONDS);
        selectLocation("City", city);
        if (!waitUntil(() -> !isFormLocked() && visible(Drawer.ADD_PRODUCT), UI_TIMEOUT_SECONDS)) {
            reportStep("The form did not unlock after the last location level was chosen", "FAIL");
        }
    }

    /** The budget month shown (read-only) in the drawer, e.g. "November 2026". */
    public String budgetMonth() {
        return valueOf(Drawer.BUDGET_MONTH_FIELD);
    }

    // ---------------------------------------------------------------- days

    public void enterDays(String days) {
        replaceText(find(Drawer.DAYS_INPUT), days);
    }

    public void clearDays() {
        clearField(find(Drawer.DAYS_INPUT));
    }

    public String days() {
        return valueOf(Drawer.DAYS_INPUT);
    }

    /** The maximum the days field allows (its max attribute), or -1. */
    public int daysMax() {
        String max = getAttribute(find(Drawer.DAYS_INPUT), "max");
        return max == null || max.isBlank() ? -1 : Integer.parseInt(max.trim());
    }

    public String daysPlaceholder() {
        String ph = getAttribute(find(Drawer.DAYS_INPUT), "placeholder");
        return ph == null ? "" : ph;
    }

    // ---------------------------------------------------------------- product rows

    public int productRowCount() {
        return count(Drawer.PRODUCT_SECTION + "//div[@role='combobox']");
    }

    public List<String> productOptions() {
        return readSelectOptions(Drawer.productSelect(1));
    }

    public void selectProduct(int row, String namePrefix) {
        selectOptionStartingWith(Drawer.productSelect(row), namePrefix);
    }

    public String selectedProduct(int row) {
        return textOf(Drawer.productSelect(row));
    }

    public void enterPlannedQuantityForSampling(int row, String value) {
        replaceText(find(Drawer.productInput(row, 2)), value);
    }

    public void enterPlannedQuantityForSales(int row, String value) {
        replaceText(find(Drawer.productInput(row, 3)), value);
    }

    public String plannedQuantityForSampling(int row) {
        return valueOf(Drawer.productInput(row, 2));
    }

    public void addProductRow() {
        click(find(Drawer.ADD_PRODUCT));
    }

    public int productDeleteIconCount() {
        return count(Drawer.PRODUCT_ROW_DELETE);
    }

    /** Deletes the n-th product row (1-based) through its trash icon; a single row has none. */
    public void deleteProductRow(int row) {
        List<WebElement> icons = getDriver().findElements(xp(Drawer.PRODUCT_ROW_DELETE));
        click(icons.get(row - 1));
    }

    // ---------------------------------------------------------------- manpower rows

    public int manpowerRowCount() {
        return count(Drawer.MANPOWER_SECTION + "//div[@role='combobox']");
    }

    public List<String> designationOptions() {
        return readSelectOptions(Drawer.manpowerSelect(1));
    }

    public void selectDesignation(int row, String namePrefix) {
        selectOptionStartingWith(Drawer.manpowerSelect(row), namePrefix);
    }

    public String selectedDesignation(int row) {
        return textOf(Drawer.manpowerSelect(row));
    }

    public void enterUnit(int row, String value) {
        replaceText(find(Drawer.manpowerInput(row, 2)), value);
    }

    public void enterRate(int row, String value) {
        replaceText(find(Drawer.manpowerInput(row, 3)), value);
    }

    public void enterManpowerDays(int row, String value) {
        replaceText(find(Drawer.manpowerInput(row, 5)), value);
    }

    public String unit(int row) {
        return valueOf(Drawer.manpowerInput(row, 2));
    }

    public String rate(int row) {
        return valueOf(Drawer.manpowerInput(row, 3));
    }

    public String standardRate(int row) {
        return valueOf(Drawer.manpowerInput(row, 4));
    }

    public String manpowerDays(int row) {
        return valueOf(Drawer.manpowerInput(row, 5));
    }

    public String lineTotal(int row) {
        return valueOf(Drawer.manpowerInput(row, 6));
    }

    public boolean isStandardRateReadOnly(int row) {
        return !enabled(Drawer.manpowerInput(row, 4));
    }

    public void addManpowerRow() {
        click(find(Drawer.ADD_MANPOWER));
    }

    public int manpowerDeleteIconCount() {
        return count(Drawer.MANPOWER_ROW_DELETE);
    }

    public void deleteManpowerRow(int row) {
        List<WebElement> icons = getDriver().findElements(xp(Drawer.MANPOWER_ROW_DELETE));
        click(icons.get(row - 1));
    }

    // ---------------------------------------------------------------- totals

    public BigDecimal manpowerTotal() {
        return parseAmount(domTextOf(Drawer.MANPOWER_TOTAL));
    }

    public BigDecimal headerRequested() {
        return parseAmount(domTextOf(Drawer.HEADER_REQUESTED));
    }

    public BigDecimal footerRequested() {
        return parseAmount(domTextOf(Drawer.FOOTER_REQUESTED));
    }

    // ---------------------------------------------------------------- buttons

    public boolean isDoneEnabled() {
        return enabled(Drawer.DONE);
    }

    public String doneCaption() {
        return domTextOf(Drawer.DONE);
    }

    /** WRITES: saves the activity to the database (a pre-draft). */
    public void clickDone() {
        click(find(Drawer.DONE));
    }

    /** Closes the drawer without saving. The portal asks for no confirmation (D-52). */
    public void close() {
        click(find(Drawer.CLOSE));
    }

    public void reset() {
        click(find(Drawer.RESET));
    }
}
