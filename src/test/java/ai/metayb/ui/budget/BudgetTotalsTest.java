package ai.metayb.ui.budget;

import ai.metayb.ui.pages.budget.BudgetActivityDrawer;
import ai.metayb.ui.pages.budget.BudgetCreatePage.Totals;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.util.ArrayList;
import java.util.List;

/**
 * D-55: do the Create Budget totals follow the activity selection and the saved state? The document records, as
 * expected behaviour (both marked "confirm" there):
 * <ul>
 *   <li>BC-090: "Grand Total = checked activities only" (an unchecked saved card must not count);</li>
 *   <li>BC-120: "Totals reflect only activities that will be saved/submitted" (an unsaved card must not count).</li>
 * </ul>
 * "Will be saved" is what the document's own messages define: Save as Draft only saves activities that are checked AND
 * were saved with Done (AL-029 'Nothing to Save - Please click "Done" on at least one checked activity first.', AL-027
 * 'x of y checked activities ready'). The amounts compared are the section "Total" and the footer "Grand Total"; the header
 * "Total Requested" is recorded as well. Controls (what the document marks as passing): a saved, checked activity counts
 * with its requested amount (BC-075, BC-085, BC-119).
 *
 * Not asserted, because the document does not settle it: the totals while an activity form is still OPEN. BC-117 expects
 * the page totals to follow the open form live, which cannot be reconciled with BC-120 without a decision from the BA.
 *
 * Every state is written to a "totals ledger" (log and Allure) so the actual figures are visible even when the test fails.
 */
@Epic("Budget")
@Feature("Budget - Create: totals")
public class BudgetTotalsTest extends BudgetFormTestBase {

    /** Activity A: 2 x 500 x 3 days = 3,000. */
    private static final long AMOUNT_A = 3000;
    /** Activity B: 1 x 500 x 2 days = 1,000. */
    private static final long AMOUNT_B = 1000;

    private final List<String> ledger = new ArrayList<>();

    private void record(String state, Totals totals) {
        String line = state + " -> " + totals;
        ledger.add(line);
        logger.info("[Totals ledger] {}", line);
    }

    private void attachLedger() {
        Allure.addAttachment("Totals ledger", "text/plain", String.join(System.lineSeparator(), ledger));
    }

    /** Fills Event Sampling with one manpower line: units x 500 x days, and the given title. */
    private void fill(BudgetActivityDrawer form, String title, int units, int days) {
        form.enterTitle(title);
        form.selectLocation(BudgetTestData.STATE, BudgetTestData.COUNTY, BudgetTestData.CITY);
        form.enterDays(String.valueOf(days));
        form.selectDesignation(1, BudgetTestData.DESIGNATION);
        form.enterUnit(1, String.valueOf(units));
        form.enterManpowerDays(1, String.valueOf(days));
        Assert.assertTrue(form.isDoneEnabled(), "Precondition: the completed form should be ready to save (Done enabled)");
    }

    /** Closes the form WITHOUT saving: the activity stays on the page as an unsaved card. */
    private void closeWithoutSaving(BudgetActivityDrawer form) {
        form.close();
        form.waitUntilClosed();
    }

    // ------------------------------------------------------------------------------------------ no writes

    @Test(groups = {"budget", "budget-readonly", "budget-defect"},
            description = "BC-120 (D-55): unsaved and unchecked activities are not counted in Total / Grand Total")
    @Story("Totals follow the selection and the saved state")
    @Severity(SeverityLevel.NORMAL)
    @Description("Known defect D-55. NOTHING IS SAVED: two activities are filled and their forms closed with no Done, so both stay 'Not saved' cards "
            + "(A = 3,000, B = 1,000). With no saved activity, nothing will be saved or submitted, so Total and Grand Total are expected to be 0 "
            + "in every state: both cards checked, then B unchecked, then A unchecked. The document records that unsaved and unchecked amounts "
            + "are included. The requirement is marked 'confirm' in the document.")
    public void bc120D55UnsavedAndUncheckedActivitiesExcludedFromTotals() {
        openCreateWithMonth();
        SoftAssert soft = new SoftAssert();
        try {
            drawer = page.addActivity(BudgetTestData.ACTIVITY);
            fill(drawer, BudgetTestData.uniqueTitle("D55a"), 2, 3);
            closeWithoutSaving(drawer);
            record("A filled, form closed, not saved (checked)", page.settledTotals(5));
            Assert.assertTrue(page.cardText(1).contains("3,000"),
                    "Precondition: the unsaved card A should show its own requested amount 3,000 (so a zero total is a real exclusion): " + page.cardText(1));
            Assert.assertFalse(page.isCardSaved(1), "Precondition: card A should not be saved: " + page.cardText(1));
            expectZero(soft, "A unsaved and checked", page.settledTotals(5), "BC-120");

            drawer = page.addActivity(BudgetTestData.ACTIVITY);
            fill(drawer, BudgetTestData.uniqueTitle("D55b"), 1, 2);
            closeWithoutSaving(drawer);
            Totals both = page.settledTotals(5);
            record("B filled, form closed, not saved (A and B checked)", both);
            Assert.assertEquals(page.cardCount(), 2, "Precondition: two cards");
            expectZero(soft, "A and B unsaved and checked", both, "BC-120");

            page.toggleCard(2);
            Totals bUnchecked = page.settledTotals(5);
            record("B unchecked", bUnchecked);
            expectZero(soft, "B unchecked, A unsaved", bUnchecked, "BC-090 / BC-120");

            page.toggleCard(1);
            Totals noneChecked = page.settledTotals(5);
            record("A and B unchecked", noneChecked);
            expectZero(soft, "nothing checked", noneChecked, "BC-090");
            soft.assertTrue(page.selectedText().contains("0 of 2"), "BC-089 '0 of 2 activities selected' but shows '" + page.selectedText() + "'");
        } finally {
            attachLedger();
        }
        soft.assertAll();
    }

    private void expectZero(SoftAssert soft, String state, Totals totals, String ref) {
        soft.assertTrue(totals.isZero(), "D-55 / " + ref + ": with " + state + " the Total and Grand Total should be 0 (nothing will be saved) but are " + totals);
    }

    // ------------------------------------------------------------------------------------------ one record is written

    @Test(groups = {"budget", "budget-write", "budget-defect"},
            description = "BC-090, BC-120 (D-55): Total / Grand Total for a saved, an unchecked and an unsaved activity")
    @Story("Totals follow the selection and the saved state")
    @Severity(SeverityLevel.NORMAL)
    @Description("WRITES one pre-draft activity (Done on activity A, 2 x 500 x 3 = 3,000); activity B (1,000) is never saved. Totals are recorded in each "
            + "state: (1) A saved and checked: 3,000 (control, BC-085 / BC-075); (2) B added but unsaved: 3,000, B excluded (BC-120); "
            + "(3) A unchecked: 0 (BC-090); (4) A checked again: 3,000 (control). Known defect D-55: the unsaved and unchecked amounts are counted. "
            + "Cleanup: the app has no list-level delete for the pre-draft; the saved card offers 'Delete activity (will also delete from database)', "
            + "which this test does NOT use (nothing is deleted); the created activity id is recorded in the log and target/budget-created-records.txt.")
    public void bc090Bc120D55TotalsForSavedUncheckedAndUnsavedActivities() {
        String titleA = BudgetTestData.uniqueTitle("D55-saved");
        openCreateWithMonth();
        SoftAssert soft = new SoftAssert();
        try {
            drawer = page.addActivity(BudgetTestData.ACTIVITY);
            fill(drawer, titleA, 2, 3);
            record("A filled, form open, not saved (observation only: BC-117 vs BC-120 is undecided)", page.totals());

            drawer.clickDone();   // WRITE: saves activity A to the database (a pre-draft)
            BudgetRecords.trackStandalone("BC-090/BC-120", "pre-draft (Done pressed)", titleA, "D-55 totals test");
            Assert.assertTrue(page.waitForCardSaved(1, 30), "Card A should show 'Saved successfully' after Done: " + page.cardText(1));
            BudgetRecords.trackStandalone("BC-090/BC-120", "pre-draft saved", titleA, "activityId=" + page.activityIdOfCard(1));

            Totals saved = page.settledTotals(5);
            record("(1) A saved and checked", saved);
            soft.assertTrue(saved.equalsAmount(AMOUNT_A), "Control: a saved, checked activity should give Total and Grand Total " + AMOUNT_A + " but they are " + saved);

            drawer = page.addActivity(BudgetTestData.ACTIVITY);
            fill(drawer, BudgetTestData.uniqueTitle("D55-unsaved"), 1, 2);
            closeWithoutSaving(drawer);
            Assert.assertEquals(page.cardCount(), 2, "Precondition: cards A and B");
            Assert.assertFalse(page.isCardSaved(2), "Precondition: card B must not be saved: " + page.cardText(2));
            Totals withUnsaved = page.settledTotals(5);
            record("(2) A saved + B unsaved (B = " + AMOUNT_B + ")", withUnsaved);
            soft.assertTrue(withUnsaved.equalsAmount(AMOUNT_A),
                    "D-55 / BC-120: the unsaved activity B (" + AMOUNT_B + ") should be excluded: Total and Grand Total should stay " + AMOUNT_A + " but are " + withUnsaved);

            page.toggleCard(1);
            Totals aUnchecked = page.settledTotals(5);
            record("(3) A unchecked (B unsaved)", aUnchecked);
            soft.assertTrue(aUnchecked.isZero(),
                    "D-55 / BC-090: with the saved activity A unchecked and B unsaved nothing will be saved, so Total and Grand Total should be 0 but are " + aUnchecked);

            page.toggleCard(1);
            Totals aChecked = page.settledTotals(5);
            record("(4) A checked again (B unsaved)", aChecked);
            soft.assertTrue(aChecked.equalsAmount(AMOUNT_A),
                    "Control: checking the saved activity A again should bring the totals back to " + AMOUNT_A + " (B still excluded) but they are " + aChecked);
        } finally {
            attachLedger();
        }
        soft.assertAll();
    }
}
