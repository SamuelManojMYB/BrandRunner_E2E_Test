package ai.metayb.ui.budget;

import ai.metayb.ui.pages.budget.BudgetActivityDrawer;
import ai.metayb.ui.pages.budget.BudgetCreatePage;
import org.testng.annotations.AfterMethod;
import testUtils.BudgetAuthenticatedTest;

/**
 * Base of the Create Budget tests that must NOT write anything. They work on Step 1, the picker and the unsaved
 * activity form, and never press Done, Save as Draft or Submit, so no record can be created. The page object of these
 * tests is always left again with Cancel, which with nothing saved creates nothing. The one exception is the
 * budget-write test of {@link BudgetTotalsTest}, which presses Done once and records the activity it creates.
 */
public abstract class BudgetFormTestBase extends BudgetAuthenticatedTest {

    protected BudgetCreatePage page;
    protected BudgetActivityDrawer drawer;

    /** Create Budget with the Country and next month chosen (so Add Activity is available). */
    protected BudgetCreatePage openCreateWithMonth() {
        page = openCreateBudget();
        page.selectCountry(BudgetTestData.COUNTRY);
        page.setBudgetMonth(BudgetTestData.testMonth());
        return page;
    }

    /** The activity form of Event Sampling, opened but with no location chosen yet. */
    protected BudgetActivityDrawer openFormBeforeLocation() {
        openCreateWithMonth();
        drawer = page.addActivity(BudgetTestData.ACTIVITY);
        return drawer;
    }

    /** The activity form of Event Sampling with the documented location chosen, so Product and Manpower are unlocked. */
    protected BudgetActivityDrawer openFormWithLocation() {
        openFormBeforeLocation();
        drawer.selectLocation(BudgetTestData.STATE, BudgetTestData.COUNTY, BudgetTestData.CITY);
        return drawer;
    }

    /** Leaves the page without saving. Never fails a test itself: it only restores a neutral state. */
    @AfterMethod(alwaysRun = true)
    public void leaveWithoutSaving() {
        try {
            if (page == null) {
                return;
            }
            BudgetActivityDrawer open = new BudgetActivityDrawer();
            if (open.isOpen()) {
                open.close();
                open.waitUntilClosed();
            }
            if (page.currentUrl().contains("/bulk-create")) {
                page.cancel();
            }
        } catch (Throwable e) {
            logger.warn("[AfterMethod] Could not leave the Create Budget page cleanly: {}", e.getMessage());
        } finally {
            page = null;
            drawer = null;
        }
    }
}
