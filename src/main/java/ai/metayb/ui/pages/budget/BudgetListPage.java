package ai.metayb.ui.pages.budget;

import ai.metayb.ui.components.budget.BudgetElements.ListPage;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The Budget list (/anp/budget): List and Table views, search, filters, city groups with their rows. Read-only: it can
 * search, filter, expand and open rows, and it can start Create Budget, but it has no way to approve, duplicate,
 * delete or otherwise change a budget.
 */
public class BudgetListPage extends BudgetPageBase {

    public static final List<String> FILTER_CAPTIONS = List.of("Country", "State", "County", "City", "Activity", "Status");
    private static final Pattern NUMBER_IN_BRACKETS = Pattern.compile("\\((\\d[\\d,]*)");
    private static final Pattern REQUEST_CODE = Pattern.compile("BGT-[A-Z0-9]{6}");

    // ---------------------------------------------------------------- opening

    public void open() {
        navigateToUrl(BudgetNavigationPage.baseUrl() + "/anp/budget");
        waitForLoaded();
    }

    /** Waits for the list shell and its "(N Total)" count, which only appears once the data has loaded. */
    public void waitForLoaded() {
        mustBeVisible(ListPage.HEADING, "The Budget list heading");
        mustBeVisible(ListPage.TOTAL, "The list total");
    }

    public boolean isDisplayed() {
        return visible(ListPage.HEADING);
    }

    // ---------------------------------------------------------------- default elements (BC-024)

    public boolean hasListToggle() {
        return visible(ListPage.LIST_TOGGLE);
    }

    public boolean hasTableToggle() {
        return visible(ListPage.TABLE_TOGGLE);
    }

    public boolean hasSearch() {
        return visible(ListPage.SEARCH);
    }

    public boolean hasExport() {
        return visible(ListPage.EXPORT);
    }

    public boolean hasCreateBudget() {
        return visible(ListPage.CREATE_BUDGET);
    }

    public boolean hasMonthYearFilter() {
        return visible(ListPage.filterLabel("Month Year"));
    }

    public boolean hasFilter(String caption) {
        return visible(ListPage.filterLabel(caption));
    }

    public boolean hasResetFilters() {
        return visible(ListPage.RESET_FILTERS);
    }

    public boolean hasApplyFilters() {
        return visible(ListPage.APPLY_FILTERS);
    }

    public boolean hasExpandAll() {
        return visible(ListPage.EXPAND_ALL);
    }

    /** The "(636 Total)" next to "Standard Activity". */
    public int getTotalCount() {
        return firstNumber(domTextOf(ListPage.TOTAL));
    }

    /** The count shown inside a filter, e.g. 9 for "All Country (9)". */
    public int getFilterCount(String caption) {
        return firstNumber(domTextOf(ListPage.filterSelect(caption)));
    }

    private static int firstNumber(String text) {
        Matcher m = NUMBER_IN_BRACKETS.matcher(text);
        if (!m.find()) {
            return -1;
        }
        return Integer.parseInt(m.group(1).replace(",", ""));
    }

    // ---------------------------------------------------------------- generic text and pagination label

    /** Whether a leaf element contains the text (used for the group breadcrumb "Country : United States" and similar). */
    public boolean isTextPresent(String text) {
        return exists("//*[not(*)][contains(normalize-space(), " + ai.metayb.ui.components.budget.BudgetElements.lit(text) + ")]");
    }

    /** Texts such as "1-100 of 636" (the count label under the list). */
    public List<String> paginationLabels() {
        return textsOf("//*[not(*)][contains(normalize-space(),' of ')][string-length(translate(normalize-space(),'0123456789',''))<string-length(normalize-space())]");
    }

    /** Waits until the "(N Total)" count differs from {@code before}; false when it did not change in time. */
    public boolean waitForTotalToChange(int before, int seconds) {
        return waitUntil(() -> getTotalCount() != before, seconds);
    }

    // ---------------------------------------------------------------- views, search, filters

    public void clickCreateBudget() {
        click(find(ListPage.CREATE_BUDGET));
    }

    public void switchToTable() {
        click(find(ListPage.TABLE_TOGGLE));
    }

    public void switchToList() {
        click(find(ListPage.LIST_TOGGLE));
    }

    /** Types into Search (React-controlled, so the field is cleared by key presses first). */
    public void search(String text) {
        WebElement box = find(ListPage.SEARCH);
        replaceText(box, text);
    }

    public void clearSearch() {
        clearField(find(ListPage.SEARCH));
    }

    public String getSearchText() {
        return valueOf(ListPage.SEARCH);
    }

    public void applyFilters() {
        click(find(ListPage.APPLY_FILTERS));
    }

    public void resetFilters() {
        click(find(ListPage.RESET_FILTERS));
    }

    /** Expands every city group (the button then reads "Collapse All"). */
    public void expandAll() {
        if (!hasExpandAll()) {
            mustBeVisible(ListPage.COLLAPSE_ALL, "The Collapse All button");   // already expanded: nothing to click
            return;
        }
        click(find(ListPage.EXPAND_ALL));
        mustBeVisible(ListPage.COLLAPSE_ALL, "The Collapse All button");
    }

    public void collapseAll() {
        click(find(ListPage.COLLAPSE_ALL));
        mustBeVisible(ListPage.EXPAND_ALL, "The Expand All button");
    }

    public String expandToggleCaption() {
        return domTextOf(ListPage.EXPAND_TOGGLE);
    }

    public boolean hasCurrencySymbol() {
        return exists(ListPage.CURRENCY_SYMBOL);
    }

    public boolean hasColumnHeader(String caption) {
        return exists(ListPage.columnHeader(caption));
    }

    /** The statuses the Status filter offers (a multi-select; the first entry is the "Deselect All" toggle). */
    public List<String> statusFilterOptions() {
        List<String> options = readSelectOptions(ListPage.filterSelect("Status"));
        options.removeIf(o -> o.isBlank() || o.startsWith("Deselect All") || o.startsWith("Select All"));
        return options;
    }

    /** Leaves only one status ticked in the Status filter (it does not apply the filter). */
    public void selectOnlyStatus(String status) {
        openSelect(ListPage.filterSelect("Status"));
        for (WebElement option : getDriver().findElements(xp(ai.metayb.ui.components.budget.BudgetElements.Popup.OPTIONS))) {
            String text = option.getText().trim();
            if (text.startsWith("Deselect All")) {
                click(option);
                break;
            }
        }
        click(find(ai.metayb.ui.components.budget.BudgetElements.Popup.option(status)));
        closeOpenSelect();
    }

    // ---------------------------------------------------------------- groups and rows

    public boolean hasGroup(String name) {
        return exists(ListPage.group(name));
    }

    public void expandGroup(String name) {
        click(find(ListPage.group(name)));
    }

    /** The number of rows currently in the DOM (rows of collapsed groups are in the DOM but not shown). */
    public int rowCountInDom() {
        return count(ListPage.ROWS);
    }

    public int visibleRowCount() {
        int shown = 0;
        for (WebElement row : getDriver().findElements(xp(ListPage.ROWS))) {
            if (row.isDisplayed()) {
                shown++;
            }
        }
        return shown;
    }

    /** The text of every visible row. */
    public List<String> visibleRowTexts() {
        List<String> texts = new java.util.ArrayList<>();
        for (WebElement row : getDriver().findElements(xp(ListPage.ROWS))) {
            if (row.isDisplayed()) {
                texts.add(row.getText().replaceAll("\\s+", " ").trim());
            }
        }
        return texts;
    }

    /**
     * Searches for a record by text and expands the result groups so its row is displayed. Returns false when no row
     * with that text appears in time (the record is not found), true when it is displayed.
     */
    public boolean findRecord(String text, int seconds) {
        int before = getTotalCount();
        search(text);
        waitForTotalToChange(before, 10);
        if (hasExpandAll()) {
            expandAll();
        }
        return waitUntil(() -> isRowVisible(text), seconds);
    }

    public boolean hasRowContaining(String text) {
        return exists(ListPage.rowContaining(text));
    }

    public boolean isRowVisible(String text) {
        return visible(ListPage.rowContaining(text));
    }

    /** The full text of the first row containing the text. */
    public String rowText(String text) {
        return domTextOf(ListPage.rowContaining(text));
    }

    /** The "BGT-XXXXXX" request code shown in the row that contains the text, or null. */
    public String requestCodeOfRow(String text) {
        Matcher m = REQUEST_CODE.matcher(rowText(text));
        return m.find() ? m.group() : null;
    }

    public void openRowView(String rowText) {
        click(find(ListPage.rowViewButton(rowText)));
    }

    public boolean rowHasButton(String rowText, String button) {
        return exists(ListPage.rowContaining(rowText) + "//button[normalize-space()=" + ai.metayb.ui.components.budget.BudgetElements.lit(button)
                + " or @aria-label=" + ai.metayb.ui.components.budget.BudgetElements.lit(button) + "]");
    }

    public void clickRowEdit(String rowText) {
        click(find(ListPage.rowEditButton(rowText)));
    }

    // ---------------------------------------------------------------- pagination (Table view)

    public boolean hasNextPage() {
        return visible(ListPage.NEXT_PAGE);
    }

    /** Whether "Go to previous page" is shown and enabled. */
    public boolean hasPreviousPage() {
        return visible(ListPage.PREVIOUS_PAGE) && enabled(ListPage.PREVIOUS_PAGE);
    }

    /** Waits until at least one row is displayed; false when none appears in time. */
    public boolean waitForRowsToBeVisible(int seconds) {
        return waitUntil(() -> visibleRowCount() > 0, seconds);
    }

    public boolean waitForTotalEquals(int expected, int seconds) {
        return waitUntil(() -> getTotalCount() == expected, seconds);
    }

    public void clickNextPage() {
        click(find(ListPage.NEXT_PAGE));
    }

    /** Escapes any open MUI popup (filter lists) so the page is left in a neutral state. */
    public void dismissPopups() {
        closeOpenSelect();
    }
}
