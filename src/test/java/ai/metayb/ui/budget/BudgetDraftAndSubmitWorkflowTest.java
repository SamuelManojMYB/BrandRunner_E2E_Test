package ai.metayb.ui.budget;

import ai.metayb.ui.pages.budget.BudgetActivityDrawer;
import ai.metayb.ui.pages.budget.BudgetCreatePage;
import ai.metayb.ui.pages.budget.BudgetListPage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import testUtils.BudgetAuthenticatedTest;

import java.math.BigDecimal;

/**
 * E2E-02 (save an activity), E2E-03 (save as draft, reopen, edit), E2E-04 (submit and verify the status) and E2E-05
 * (find the record in the list) of the Budget test plan, as ONE serial workflow that creates ONE budget:
 *
 *   Create Budget -> activity filled -> Done (pre-draft) -> Save as Draft -> list -> reopen and edit -> Submit -> list/search.
 *
 * These tests WRITE to the shared QA backend, so they follow strict rules:
 * - one uniquely titled record (QA-Claude-Yeppie-E2E-&lt;timestamp&gt;) is created per run and reused by every step;
 * - every created record is tracked in {@link BudgetRecords} (log, Allure attachment, target/budget-created-records.txt);
 * - nothing is ever deleted: the delete dialog is only opened and declined ("Keep it"), and the delete cases are skipped;
 * - a submitted budget cannot be withdrawn by the creator, so its Request ID is reported for manual cleanup.
 *
 * Known defects are asserted against the documented expectation (D-54, D-55, D-65, D-66, D-63); a failing step does not stop
 * the later steps (alwaysRun), which re-open the record from the list instead of relying on the previous page.
 */
@Epic("Budget")
@Feature("Budget - Draft and submit workflow (E2E-02 to E2E-05)")
public class BudgetDraftAndSubmitWorkflowTest extends BudgetAuthenticatedTest {

    private static final String TITLE = BudgetTestData.uniqueTitle("E2E");
    private static final BigDecimal THREE_THOUSAND = new BigDecimal("3000");
    private static final BigDecimal FOUR_THOUSAND = new BigDecimal("4000");
    private static final String MONTH_LABEL = BudgetTestData.monthLabel(BudgetTestData.testMonth());

    private BudgetCreatePage page;

    // ------------------------------------------------------------------------------------------ E2E-02

    @Test(groups = {"budget", "budget-write"},
            description = "E2E-02 (BC-085..BC-090, BC-093, AL-024, AL-026, AL-027, D-55): create an activity and save it with Done")
    @Story("E2E-02 Create budget and validate activities")
    @Severity(SeverityLevel.BLOCKER)
    @Description("WRITES one pre-draft activity record. Fills Event Sampling (location, 3 days, product, 2 x 500 x 3 manpower), presses Done and checks "
            + "the saved card, View, Edit with Update & Done, the delete dialog (declined with 'Keep it'), and Uncheck / the grand total (D-55).")
    public void e2e02CreateActivityAndSaveWithDone() {
        page = openCreateBudget();
        page.selectCountry(BudgetTestData.COUNTRY);
        page.setBudgetMonth(BudgetTestData.testMonth());
        BudgetActivityDrawer drawer = page.addActivity(BudgetTestData.ACTIVITY);
        drawer.enterTitle(TITLE);
        drawer.selectLocation(BudgetTestData.STATE, BudgetTestData.COUNTY, BudgetTestData.CITY);
        drawer.enterDays("3");
        drawer.selectProduct(1, BudgetTestData.PRODUCT);
        drawer.enterPlannedQuantityForSampling(1, "100");
        drawer.selectDesignation(1, BudgetTestData.DESIGNATION);
        drawer.enterUnit(1, "2");
        drawer.enterManpowerDays(1, "3");
        Assert.assertTrue(drawer.isDoneEnabled(), "The completed form should be ready to save (Done enabled)");
        Assert.assertEquals(drawer.headerRequested().compareTo(THREE_THOUSAND), 0, "Requested amount before saving");

        drawer.clickDone();   // WRITE: saves the activity to the database (pre-draft)
        BudgetRecords.track("E2E-02", "pre-draft (Done pressed)", TITLE, null, null);
        Assert.assertTrue(page.waitForCardSaved(1, 30), "The card should show 'Saved successfully' after Done: " + page.cardText(1));
        String activityId = page.activityIdOfCard(1);
        BudgetRecords.track("E2E-02", "pre-draft saved (activity #" + activityId + ")", TITLE, null, null);

        SoftAssert soft = new SoftAssert();
        // BC-085, AL-026, AL-027
        String card = page.cardText(1);
        soft.assertTrue(new BudgetActivityDrawer().isOpen() == false, "BC-085 Done closes the form");
        soft.assertTrue(card.contains(TITLE), "BC-085 the card shows the title: " + card);
        soft.assertTrue(card.contains("3,000"), "BC-085 the card shows the requested amount 3,000: " + card);
        soft.assertTrue(card.contains("3 days") && card.contains("BM: " + MONTH_LABEL), "BC-085 the card shows '3 days' and 'BM: " + MONTH_LABEL + "': " + card);
        soft.assertNotNull(activityId, "BC-085 the card shows the saved id (#...)");
        soft.assertTrue(page.readyText().contains("1 of 1") && page.readyText().contains("ready"), "AL-027 '1 of 1 checked activity ready' but shows '" + page.readyText() + "'");
        soft.assertTrue(page.isSaveAsDraftEnabled() && page.saveAsDraftCaption().contains("(1)"), "BC-085 Save as Draft (1) enabled: " + page.saveAsDraftCaption());
        soft.assertTrue(page.isSubmitEnabled() && page.submitCaption().contains("(1)"), "BC-085 Submit (1) enabled: " + page.submitCaption());

        // BC-086: View expands the card read-only
        page.viewCard(1);
        String viewed = page.cardText(1);
        soft.assertTrue(viewed.contains("Saved (pre-draft)") && viewed.contains(BudgetTestData.CITY), "BC-086 View shows the saved details: " + viewed);

        // BC-087, BC-088: Edit reloads the saved values; Update & Done keeps the same id
        BudgetActivityDrawer edit = page.editCard(1);
        soft.assertEquals(edit.title(), TITLE, "BC-087 the title is pre-filled");
        soft.assertEquals(edit.days(), "3", "BC-087 the days are pre-filled");
        soft.assertTrue(edit.selectedDesignation(1).startsWith(BudgetTestData.DESIGNATION), "BC-087 the designation is pre-filled: " + edit.selectedDesignation(1));
        soft.assertEquals(edit.doneCaption(), "Update & Done", "BC-087 the button reads 'Update & Done'");
        edit.enterPlannedQuantityForSales(1, "5");
        edit.clickDone();   // WRITE: updates the same activity
        edit.waitUntilClosed();
        soft.assertTrue(page.waitForCardSaved(1, 30), "BC-088 the card is saved again after Update & Done");
        soft.assertEquals(page.activityIdOfCard(1), activityId, "BC-088 the same #id is kept after Update & Done");

        // BC-093 / AL-024: the delete dialog for a saved activity is opened and DECLINED (nothing is deleted)
        page.clickRemoveCard(1);
        soft.assertTrue(page.isDeleteConfirmationShown(), "BC-093 'Delete Activity?' dialog");
        soft.assertTrue(page.isTextShown("has already been saved to the database"), "AL-024 the dialog explains the activity is saved to the database");
        if (page.isDeleteConfirmationShown()) {
            page.clickKeepIt();
        }
        soft.assertEquals(page.cardCount(), 1, "BC-093 'Keep it' keeps the card");

        // BC-089, BC-090 (D-55): unchecking leaves nothing to save, and the grand total should then be 0
        page.toggleCard(1);
        soft.assertFalse(page.isSaveAsDraftEnabled(), "BC-089 Save as Draft is disabled with nothing checked");
        soft.assertFalse(page.isSubmitEnabled(), "BC-089 Submit is disabled with nothing checked");
        soft.assertTrue(page.selectedText().contains("0 of 1"), "BC-089 '0 of 1 ... selected' but shows '" + page.selectedText() + "'");
        BigDecimal totalWhenUnchecked = page.grandTotal();
        soft.assertTrue(totalWhenUnchecked != null && totalWhenUnchecked.signum() == 0,
                "D-55 / BC-090 the Grand Total should count checked activities only (0 with none checked) but is " + totalWhenUnchecked);
        page.toggleCard(1);
        soft.assertTrue(page.isCardChecked(1) && page.isSaveAsDraftEnabled(), "The activity is checked again and can be saved");
        soft.assertAll();
    }

    // ------------------------------------------------------------------------------------------ E2E-03

    @Test(groups = {"budget", "budget-write", "budget-alerts"}, dependsOnMethods = "e2e02CreateActivityAndSaveWithDone", alwaysRun = true,
            description = "E2E-03 (BC-096, AL-030): Save as Draft shows 'Draft Saved' and opens the draft in Edit Bulk Budget")
    @Story("E2E-03 Save draft and verify persistence")
    @Severity(SeverityLevel.BLOCKER)
    @Description("WRITES: turns the saved activity into a draft budget.")
    public void e2e03Bc096SaveAsDraft() {
        if (page == null || !page.isCardSaved(1)) {
            throw new SkipException("E2E-02 did not leave a saved activity on the page, so there is nothing to save as a draft");
        }
        page.saveAsDraft();   // WRITE
        BudgetRecords.track("E2E-03", "draft (Save as Draft pressed)", TITLE, null, null);
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(page.waitForText("Draft Saved", 15), "BC-096 / AL-030 the 'Draft Saved' dialog");
        soft.assertTrue(page.isTextShown("1 budget(s) saved as draft successfully"), "BC-096 '1 budget(s) saved as draft successfully'");
        boolean redirected = page.waitUntilUrlContains("/anp/budget/bulk-edit/", 20);
        soft.assertTrue(redirected, "BC-096 the app should redirect to /anp/budget/bulk-edit/{id} but is on " + page.currentUrl());
        String group = page.groupIdFromUrl();
        soft.assertNotNull(group, "BC-096 the draft's group id is in the URL");
        BudgetRecords.track("E2E-03", "draft", TITLE, group, null);
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-write", "budget-defect", "budget-alerts"}, dependsOnMethods = "e2e03Bc096SaveAsDraft", alwaysRun = true,
            description = "BC-097, AL-030 (D-54): the 'Draft Saved' dialog can be closed with OK")
    @Story("Draft Saved dialog")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Known defect D-54: OK / No / Cancel become disabled and the dialog persists until the page is reloaded. The page is reloaded afterwards, "
            + "as the document prescribes, so the later steps can continue.")
    public void bc097D54DraftSavedDialogCanBeClosed() {
        if (page == null || BudgetRecords.groupId() == null) {
            throw new SkipException("No draft was saved, so there is no dialog to close");
        }
        try {
            Assert.assertTrue(page.isTextShown("Draft Saved"), "The 'Draft Saved' dialog should be showing after Save as Draft");
            boolean okEnabled = page.isOkButtonEnabled();
            if (okEnabled) {
                page.clickOk();
            }
            boolean closed = page.waitUntilTextGone("Draft Saved", 5);
            Assert.assertTrue(closed, "D-54: the 'Draft Saved' dialog cannot be closed (OK enabled: " + okEnabled + ")");
        } finally {
            page.refreshPage();
        }
    }

    // ------------------------------------------------------------------------------------------ list

    @Test(groups = {"budget", "budget-write", "budget-defect"}, dependsOnMethods = "bc097D54DraftSavedDialogCanBeClosed", alwaysRun = true,
            description = "BC-098 (D-65): the draft is listed with status Draft and Edit and Delete actions")
    @Story("Draft in the list")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Known defect D-65: the draft row has View, Duplicate and Edit but no Delete. The expectation is Edit and Delete.")
    public void bc098D65DraftListedWithEditAndDelete() {
        requireDraft();
        BudgetListPage list = openBudgetList();
        Assert.assertTrue(list.findRecord(TITLE, 30), "BC-098 the draft '" + TITLE + "' should be listed");
        String row = list.rowText(TITLE);
        String code = list.requestCodeOfRow(TITLE);
        BudgetRecords.track("BC-098", "draft", TITLE, null, code);
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(row.contains("Draft"), "BC-098 the status is Draft: " + row);
        soft.assertNotNull(code, "BC-098 the row shows a Request Id (BGT-...)");
        soft.assertTrue(list.rowHasButton(TITLE, "Edit"), "BC-098 the Draft row offers Edit");
        soft.assertTrue(list.rowHasButton(TITLE, "Delete"), "D-65 the Draft row should offer Delete");
        soft.assertAll();
    }

    // ------------------------------------------------------------------------------------------ resume and edit

    @Test(groups = {"budget", "budget-write", "budget-alerts"}, dependsOnMethods = "bc098D65DraftListedWithEditAndDelete", alwaysRun = true,
            description = "E2E-03 (BC-099, BC-119, BC-123, AL-033, AL-034): reopen the draft, verify what was saved, edit it and update")
    @Story("E2E-03 Save draft and verify persistence")
    @Severity(SeverityLevel.CRITICAL)
    @Description("WRITES: reopens the draft from the list, checks the persisted values, checks the required-field messages, then changes days 3 to 4 "
            + "(total 3,000 to 4,000) with Update & Done.")
    public void e2e03Bc099ResumeDraftAndEdit() {
        requireDraft();
        BudgetListPage list = openBudgetList();
        Assert.assertTrue(list.findRecord(TITLE, 30), "The draft should be listed");
        list.clickRowEdit(TITLE);
        page = new BudgetCreatePage();
        Assert.assertTrue(page.waitUntilUrlContains("/anp/budget/bulk-edit/", 30), "Edit should open /anp/budget/bulk-edit/{id} but is on " + page.currentUrl());
        Assert.assertTrue(page.waitForCards(30), "Edit Bulk Budget should show the draft's activity");

        SoftAssert soft = new SoftAssert();
        soft.assertEquals(page.groupIdFromUrl(), BudgetRecords.groupId(), "BC-099 the same draft is opened");
        soft.assertTrue(page.isTextShown("Budget Month: " + MONTH_LABEL) && !page.isMonthInputPresent(), "BC-123 the month is a read-only chip 'Budget Month: " + MONTH_LABEL + "'");
        soft.assertTrue(page.cardText(1).contains(TITLE) && page.cardText(1).contains("3,000"), "E2E-03 the saved title and amount persist: " + page.cardText(1));

        BudgetActivityDrawer drawer = page.editCard(1);
        soft.assertEquals(drawer.title(), TITLE, "E2E-03 the title persists");
        soft.assertEquals(drawer.days(), "3", "E2E-03 the days persist");
        soft.assertTrue(drawer.selectedProduct(1).startsWith(BudgetTestData.PRODUCT), "E2E-03 the product persists: " + drawer.selectedProduct(1));
        soft.assertTrue(drawer.selectedDesignation(1).startsWith(BudgetTestData.DESIGNATION), "E2E-03 the designation persists: " + drawer.selectedDesignation(1));
        soft.assertEquals(drawer.unit(1), "2", "E2E-03 the manpower unit persists");

        // AL-033, AL-034: required-field messages on the Edit form (the Update button must not save while they show)
        drawer.clearTitle();
        soft.assertTrue(drawer.isTextShown("Title is required") && !drawer.isDoneEnabled(), "AL-033 '⚠ Title is required' and Update & Done disabled");
        drawer.enterTitle(TITLE);
        drawer.clearDays();
        soft.assertTrue(drawer.isTextShown("Enter number of days") && !drawer.isDoneEnabled(), "AL-034 '⚠ Enter number of days' and Update & Done disabled");

        // BC-099 / BC-119: edit days 3 -> 4 (manpower days 4) and update
        drawer.enterDays("4");
        drawer.enterManpowerDays(1, "4");
        drawer.clickDone();   // WRITE: updates the draft's activity
        drawer.waitUntilClosed();
        BudgetRecords.track("BC-099", "draft updated (days 4, total 4000)", TITLE, BudgetRecords.groupId(), BudgetRecords.requestCode());
        soft.assertTrue(page.waitForCardSaved(1, 30), "BC-119 the card is saved after Update & Done");
        soft.assertTrue(page.cardText(1).contains("4,000"), "BC-119 the card shows 4,000: " + page.cardText(1));
        soft.assertEquals(page.grandTotal().compareTo(FOUR_THOUSAND), 0, "BC-099 / BC-119 Grand Total is 4,000 but is " + page.grandTotal());
        soft.assertAll();
    }

    @Test(groups = {"budget", "budget-write", "budget-defect", "budget-alerts"}, dependsOnMethods = "e2e03Bc099ResumeDraftAndEdit", alwaysRun = true,
            description = "BC-124, BC-125, AL-035 (D-63): Add Activity on Edit Bulk Budget opens the form, adds one card per click, and an unsaved card can be removed")
    @Story("Edit Bulk Budget")
    @Severity(SeverityLevel.NORMAL)
    @Description("Known defect D-63: no form opens, a 'New - not saved yet' card is added and repeated clicks add more unsaved cards. The card is removed again; "
            + "nothing is saved.")
    public void bc124Bc125Al035D63AddActivityOnEditPage() {
        if (page == null || !page.currentUrl().contains("/bulk-edit/")) {
            throw new SkipException("The draft is not open in Edit Bulk Budget");
        }
        int before = page.cardCount();
        page.addActivityFromPicker(BudgetTestData.ACTIVITY);
        boolean formOpened = page.isActivityFormOpenWithin(5);
        if (formOpened) {
            BudgetActivityDrawer open = new BudgetActivityDrawer();
            open.close();
            open.waitUntilClosed();
        }
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(formOpened, "D-63 / BC-124 Add Activity should open the activity form, as on Create");
        soft.assertEquals(page.cardCount(), before + 1, "BC-124 one new card per click");
        soft.assertTrue(page.isTextShown("unsaved activit"), "AL-035 the footer '⚠ N unsaved activities - save them first'");
        if (page.cardCount() > before) {
            page.clickRemoveCard(page.cardCount());
            soft.assertEquals(page.cardCount(), before, "BC-125 the unsaved card is removed and the counter decreases");
        }
        soft.assertAll();
    }

    // ------------------------------------------------------------------------------------------ E2E-04

    @Test(groups = {"budget", "budget-write", "budget-defect", "budget-alerts"},
            dependsOnMethods = {"e2e03Bc099ResumeDraftAndEdit", "bc124Bc125Al035D63AddActivityOnEditPage"}, alwaysRun = true,
            description = "E2E-04 (BC-101, BC-102, BC-106, AL-037, D-66): submit the budget, then verify the success feedback, the status and the Request ID")
    @Story("E2E-04 Submit budget and verify status")
    @Severity(SeverityLevel.BLOCKER)
    @Description("WRITES and cannot be undone by the creator: submits the draft. Verifies the confirmation and the success message the document expects (D-66), "
            + "then, from the list, the status 'Pre Activity Pending', the Request ID, the amount and that the creator cannot edit it. "
            + "The Request ID is reported for manual cleanup.")
    public void e2e04Bc101Bc102Al037D66SubmitAndVerifyStatus() {
        requireDraft();
        BudgetListPage list = openBudgetList();
        Assert.assertTrue(list.findRecord(TITLE, 30), "The draft should still be listed before it is submitted");
        String codeBefore = list.requestCodeOfRow(TITLE);
        list.clickRowEdit(TITLE);
        page = new BudgetCreatePage();
        Assert.assertTrue(page.waitUntilUrlContains("/anp/budget/bulk-edit/", 30), "Edit should open the draft but is on " + page.currentUrl());
        Assert.assertTrue(page.waitForCards(30), "The draft's activity should be shown");
        Assert.assertTrue(page.isSubmitEnabled() && page.submitCaption().contains("(1)"), "Submit (1) should be enabled: " + page.submitCaption());

        page.submit();   // WRITE: not reversible by the creator
        BudgetRecords.track("E2E-04", "submit pressed", TITLE, BudgetRecords.groupId(), codeBefore);
        boolean confirmationShown = page.confirmSubmissionIfAsked(3);
        boolean leftEditPage = page.waitUntilUrlNoLonger("/bulk-edit", 30);
        boolean successMessage = page.waitForText("Budget submitted successfully", 5);

        SoftAssert soft = new SoftAssert();
        soft.assertTrue(leftEditPage, "BC-101 the app should leave the edit page after submitting but is on " + page.currentUrl());
        soft.assertTrue(confirmationShown, "BC-102 / D-66 a confirmation dialog should appear before submitting");
        soft.assertTrue(successMessage, "AL-037 / D-66 the success message 'Budget submitted successfully' should be shown after submitting");

        BudgetListPage after = new BudgetListPage();
        after.waitForLoaded();
        Assert.assertTrue(after.findRecord(TITLE, 30), "BC-101 the submitted budget should be listed");
        String row = after.rowText(TITLE);
        String code = after.requestCodeOfRow(TITLE);
        BudgetRecords.track("E2E-04", "submitted", TITLE, BudgetRecords.groupId(), code);
        soft.assertTrue(row.contains("Pre Activity Pending"), "BC-101 the status should be 'Pre Activity Pending': " + row);
        soft.assertNotNull(code, "BC-101 the submitted budget has a Request ID (BGT-...)");
        soft.assertEquals(code, codeBefore, "The budget code is unchanged by submitting");
        soft.assertTrue(row.contains("4,000"), "The amount is 4,000 as saved by the edit: " + row);
        soft.assertFalse(after.rowHasButton(TITLE, "Edit"), "BC-106 a submitted budget is not editable by its creator (no Edit action)");
        soft.assertAll();
    }

    // ------------------------------------------------------------------------------------------ E2E-05

    @Test(groups = {"budget", "budget-write"}, dependsOnMethods = "e2e04Bc101Bc102Al037D66SubmitAndVerifyStatus", alwaysRun = true,
            description = "E2E-05 (BC-030): find the created budget by title, filter it by status, open it and check its status and details")
    @Story("E2E-05 Budget list and search")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Read-only on the record created above. Searching by Request ID is logged only: the document does not say it is a supported search key.")
    public void e2e05FindCreatedBudgetBySearchAndStatusFilter() {
        requireDraft();
        BudgetListPage list = openBudgetList();
        SoftAssert soft = new SoftAssert();

        soft.assertTrue(list.findRecord(TITLE, 30), "BC-030 searching the unique title should list the created budget");
        if (list.isRowVisible(TITLE)) {
            String row = list.rowText(TITLE);
            soft.assertTrue(row.contains(BudgetRecords.isSubmitted() ? "Pre Activity Pending" : "Draft"), "The row shows the budget's current status: " + row);
            soft.assertTrue(row.contains(BudgetTestData.CURRENCY), "The row shows its amount in " + BudgetTestData.CURRENCY + ": " + row);
            soft.assertNotNull(list.requestCodeOfRow(TITLE), "The row shows the Request ID");
            for (String other : list.visibleRowTexts()) {
                soft.assertTrue(other.toLowerCase().contains(TITLE.toLowerCase()), "BC-030 the search listed a row that does not match the title: " + other);
            }
        }
        list.clearSearch();

        String code = BudgetRecords.requestCode();
        if (code != null) {
            int before = list.getTotalCount();
            list.search(code);
            list.waitForTotalToChange(before, 10);
            list.reportStep("Search by Request ID " + code + " lists " + list.getTotalCount() + " budget(s) (informational: the document does not define this key)", "INFO");
            list.clearSearch();
        }

        // filter by status, then open the record
        String status = BudgetRecords.isSubmitted() ? "Pre Activity Pending" : "Draft";
        list.selectOnlyStatus(status);
        list.applyFilters();
        list.waitForTotalToChange(list.getTotalCount() + 1, 10);
        list.expandAll();
        soft.assertTrue(list.isTextPresent(TITLE) || list.hasRowContaining(TITLE) || list.findRecord(TITLE, 20), "BC-033 filtering by '" + status + "' should list the created budget");
        list.resetFilters();
        soft.assertAll();
    }

    // ------------------------------------------------------------------------------------------ listed, not automated

    @Test(groups = {"budget", "budget-blocked"}, description = "BC-094, BC-118 (D-59): delete a saved activity; totals and the card update at once")
    @Story("Delete")
    public void bc094Bc118D59DeleteSavedActivity() {
        throw new SkipException("BC-094 / BC-118 / D-59 permanently delete a saved activity. Deleting records is out of scope for now (instruction: do not delete); "
                + "the delete dialog itself is opened and declined in E2E-02 (BC-093, AL-024).");
    }

    @Test(groups = {"budget", "budget-blocked", "budget-defect"}, description = "BC-100 (D-65): delete a draft from the list or the edit page")
    @Story("Delete")
    public void bc100D65DeleteDraft() {
        throw new SkipException("BC-100: the document records no delete action for drafts (D-65), and deleting is out of scope. D-65 is asserted in BC-098.");
    }

    @Test(groups = {"budget", "budget-blocked"}, description = "BC-103, BC-104, BC-105, BC-107: submit several / not-ready / double-click / duplicate")
    @Story("Submit variants")
    public void bc103Bc104Bc105Bc107SubmitVariants() {
        throw new SkipException("BC-103..BC-105 and BC-107 are 'Not Run' in the document and each creates additional budgets (three, a duplicate submission, a copy). "
                + "No extra backend records are created beyond the single workflow record.");
    }

    @Test(groups = {"budget", "budget-blocked"}, description = "AL-028, AL-029, AL-031: 'No Activities Saved' / 'Nothing to Save' / 'No Activities Selected' dialogs")
    @Story("Alerts")
    public void al028Al029Al031SaveWithNothingToSave() {
        throw new SkipException("AL-028, AL-029 and AL-031 are 'prevented in UI' in the document: the Save / Submit buttons are disabled so the dialogs cannot be reached. "
                + "The disabled state is asserted in E2E-01 and E2E-02 (BC-089).");
    }

    @Test(groups = {"budget", "budget-blocked"}, description = "AL-038, AL-039: incomplete card on submit; past-month draft")
    @Story("Alerts")
    public void al038Al039IncompleteSubmitAndPastMonthDraft() {
        throw new SkipException("AL-038 and AL-039 are 'Not Run' and need a specially prepared record (an incomplete card kept checked; a draft whose month is now past), "
                + "which cannot be created without additional backend records.");
    }

    // ------------------------------------------------------------------------------------------ support

    private void requireDraft() {
        if (BudgetRecords.groupId() == null) {
            throw new SkipException("E2E-03 did not create a draft, so there is no record to continue with");
        }
    }
}
