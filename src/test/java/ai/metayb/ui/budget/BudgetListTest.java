package ai.metayb.ui.budget;

import ai.metayb.ui.pages.budget.BudgetDashboardPage;
import ai.metayb.ui.pages.budget.BudgetListPage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import testUtils.BudgetAuthenticatedTest;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Budget list (section 4 of the test document, BC-024 to BC-035, plus BC-112). Read-only: the tests search, filter,
 * expand and page through existing budgets and never change or approve any of them. Searches and filters are put back
 * after each test.
 */
@Epic("Budget")
@Feature("Budget - List, search and filters")
public class BudgetListTest extends BudgetAuthenticatedTest {

    private BudgetListPage list;

    @AfterMethod(alwaysRun = true)
    public void leaveListNeutral() {
        try {
            if (list != null && list.isDisplayed()) {
                list.dismissPopups();
            }
        } catch (Throwable e) {
            logger.warn("[AfterMethod] Could not neutralise the Budget list: {}", e.getMessage());
        } finally {
            list = null;
        }
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-024: the list view shows all its default elements")
    @Story("List layout")
    @Severity(SeverityLevel.NORMAL)
    public void bc024ListDefaultElements() {
        list = openBudgetList();
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(list.hasListToggle(), "List toggle");
        soft.assertTrue(list.hasTableToggle(), "Table toggle");
        soft.assertTrue(list.hasSearch(), "Search");
        soft.assertTrue(list.hasExport(), "Export");
        soft.assertTrue(list.hasCreateBudget(), "Create Budget");
        soft.assertTrue(list.hasMonthYearFilter(), "Month Year filter");
        for (String caption : BudgetListPage.FILTER_CAPTIONS) {
            soft.assertTrue(list.hasFilter(caption), "Filter '" + caption + "'");
        }
        soft.assertTrue(list.hasResetFilters(), "Reset Filters");
        soft.assertTrue(list.hasApplyFilters(), "Apply Filters");
        soft.assertTrue(list.hasExpandAll(), "Expand All");
        soft.assertTrue(list.getTotalCount() > 0, "'Standard Activity (N Total)' should show a total");
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-025, BC-112: budgets are grouped by city with a location breadcrumb, and rows show their actions")
    @Story("List grouping")
    @Severity(SeverityLevel.NORMAL)
    public void bc025Bc112GroupingAndLocationBreadcrumb() {
        list = openBudgetList();
        if (!list.hasGroup(BudgetTestData.CITY)) {
            throw new SkipException("The list has no '" + BudgetTestData.CITY + "' group on this tenant, so the grouping cannot be checked there");
        }
        list.expandGroup(BudgetTestData.CITY);
        Assert.assertTrue(list.isTextPresent("Country : " + BudgetTestData.COUNTRY), "BC-112 breadcrumb 'Country : " + BudgetTestData.COUNTRY + "'");
        Assert.assertTrue(list.isTextPresent("State : " + BudgetTestData.STATE), "BC-112 breadcrumb 'State : " + BudgetTestData.STATE + "'");
        Assert.assertTrue(list.isTextPresent("County : " + BudgetTestData.COUNTY), "BC-112 breadcrumb 'County : " + BudgetTestData.COUNTY + "'");
        Assert.assertTrue(list.visibleRowCount() > 0, "Expanding the group should show its budget rows");
        List<String> rows = list.visibleRowTexts();
        Assert.assertTrue(rows.stream().anyMatch(r -> r.contains("BGT-")), "Rows should show a Request Id (BGT-...)");
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-026: Expand All expands every group and the same button collapses them")
    @Story("List grouping")
    @Severity(SeverityLevel.MINOR)
    public void bc026ExpandAllAndCollapse() {
        list = openBudgetList();
        Assert.assertEquals(list.visibleRowCount(), 0, "Groups start collapsed, so no row is shown");
        list.expandAll();
        Assert.assertTrue(list.visibleRowCount() > 0, "Expand All should show rows");
        Assert.assertEquals(list.expandToggleCaption(), "Collapse All", "The button reads 'Collapse All' once expanded");
        list.collapseAll();
        Assert.assertEquals(list.visibleRowCount(), 0, "Collapse All should hide the rows again");
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-027: Table view shows a flat list with Country/State/County/City columns")
    @Story("Table view")
    @Severity(SeverityLevel.NORMAL)
    public void bc027TableView() {
        list = openBudgetList();
        list.switchToTable();
        SoftAssert soft = new SoftAssert();
        for (String column : List.of("Country", "State", "County", "City")) {
            soft.assertTrue(list.hasColumnHeader(column), "Table column '" + column + "'");
        }
        int rows = list.visibleRowCount();
        soft.assertTrue(rows > 0 && rows <= 20, "Table view should show up to 20 rows per page but shows " + rows);
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly", "budget-defect"}, description = "BC-028 (D-47): Next page in Table view shows the next rows with Previous enabled")
    @Story("Pagination")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Known defect D-47: the API returns page 2 but the list and the pagination disappear. The expectation is rows 21-40 and Previous enabled.")
    public void bc028D47NextPageInTableView() {
        list = openBudgetList();
        list.switchToTable();
        Assert.assertTrue(list.hasNextPage(), "Table view should offer 'Go to next page'");
        list.clickNextPage();
        boolean rowsShown = list.waitForRowsToBeVisible(10);
        Assert.assertTrue(rowsShown, "D-47: after 'Go to next page' no rows are displayed any more");
        Assert.assertTrue(list.hasPreviousPage(), "D-47: the pagination should still be shown with Previous available after Next");
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-029: list view shows its count label and paging")
    @Story("Pagination")
    @Severity(SeverityLevel.NORMAL)
    @Description("Reads the 'x-y of N' label. Changing rows per page is not exercised: the document marks that step as not executed.")
    public void bc029ListPageSizeLabel() {
        list = openBudgetList();
        Pattern label = Pattern.compile("^1\\s*[\\u2013-]\\s*\\d+ of \\d[\\d,]*$");
        List<String> labels = list.paginationLabels();
        Assert.assertTrue(labels.stream().anyMatch(t -> label.matcher(t).matches()),
                "The list should show a count label such as '1-100 of N' but shows " + labels);
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-030: searching by name lists only matching budgets")
    @Story("Search")
    @Severity(SeverityLevel.CRITICAL)
    public void bc030SearchByName() {
        list = openBudgetList();
        int before = list.getTotalCount();
        list.search("QA-Claude");
        list.waitForTotalToChange(before, 8);
        list.expandAll();
        List<String> rows = list.visibleRowTexts();
        for (String row : rows) {
            Assert.assertTrue(row.toLowerCase().contains("qa-claude"), "Search 'QA-Claude' listed a row that does not match it: " + row);
        }
        list.clearSearch();
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-031: a search with no match lists nothing")
    @Story("Search")
    @Severity(SeverityLevel.MINOR)
    @Description("The document gives no wording for the empty state, so only the absence of rows is asserted.")
    public void bc031SearchWithNoMatch() {
        list = openBudgetList();
        int before = list.getTotalCount();
        list.search("zzqq-nomatch");
        list.waitForTotalToChange(before, 8);
        if (list.hasExpandAll()) {
            list.expandAll();
        }
        Assert.assertEquals(list.visibleRowCount(), 0, "A search with no match should list no budget");
        list.clearSearch();
    }

    @Test(groups = {"budget", "budget-readonly", "budget-defect"}, description = "BC-032 (D-58): filter counts on the list equal the dashboard's")
    @Story("Filters")
    @Severity(SeverityLevel.MINOR)
    public void bc032D58FilterCountsMatchDashboard() {
        list = openBudgetList();
        List<String> captions = List.of("Country", "State", "County", "City", "Activity");
        int[] listCounts = new int[captions.size()];
        for (int i = 0; i < captions.size(); i++) {
            listCounts[i] = list.getFilterCount(captions.get(i));
        }
        BudgetDashboardPage dashboard = new BudgetDashboardPage();
        dashboard.open();
        SoftAssert soft = new SoftAssert();
        for (int i = 0; i < captions.size(); i++) {
            if (!dashboard.hasFilter(captions.get(i))) {
                throw new SkipException("The dashboard shows no '" + captions.get(i) + "' filter, so its count cannot be compared");
            }
            soft.assertEquals(listCounts[i], dashboard.filterCount(captions.get(i)),
                    "D-58 master count of '" + captions.get(i) + "' (Budget list vs dashboard)");
        }
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-readonly"}, description = "BC-033: Apply Filters narrows the list by Status and Reset Filters restores it")
    @Story("Filters")
    @Severity(SeverityLevel.NORMAL)
    @Description("Uses the Status filter only (Draft); the Month and Country filters are not exercised.")
    public void bc033ApplyAndResetFilters() {
        list = openBudgetList();
        int all = list.getTotalCount();
        list.selectOnlyStatus("Draft");
        list.applyFilters();
        list.waitForTotalToChange(all, 10);
        int filtered = list.getTotalCount();
        Assert.assertTrue(filtered <= all, "Filtering by Draft cannot show more budgets (" + filtered + ") than all (" + all + ")");
        if (filtered > 0) {
            list.expandAll();
            for (String row : list.visibleRowTexts()) {
                Assert.assertTrue(row.contains("Draft"), "Status filter 'Draft' listed a row with another status: " + row);
            }
        }
        list.resetFilters();
        Assert.assertTrue(list.waitForTotalEquals(all, 15), "Reset Filters should restore the full list of " + all + " budgets");
    }

    @Test(groups = {"budget", "budget-blocked"}, description = "BC-034: Export downloads an Excel file with the filtered rows")
    @Story("Export")
    public void bc034ExportBudgets() {
        throw new SkipException("BC-034 is a file download; the framework has no download-verification support and the document marks it not executed.");
    }

    @Test(groups = {"budget", "budget-blocked", "budget-defect"}, description = "BC-035 (D-46): amounts in the list stay within business limits")
    @Story("Amounts")
    public void bc035D46ListAmountsWithinBusinessLimits() {
        throw new SkipException("BC-035 / D-46 expects amounts 'within business limits' but the document defines no maximum (it states there is no max limit). "
                + "The limit is asserted where the document gives data for it: BC-080 (a 14-digit rate must be rejected).");
    }
}
