package ai.metayb.ui.pages.budget;

import ai.metayb.ui.components.budget.BudgetElements.Create;
import ai.metayb.ui.components.budget.BudgetElements.Picker;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Create Budget (/anp/budget/bulk-create) and, with the same cards and footer, Edit Bulk Budget
 * (/anp/budget/bulk-edit/{id}). Step 1 (Country and Budget Month), the Add Activity picker, the activity cards and the
 * footer. The activity form itself is {@link BudgetActivityDrawer}.
 *
 * Write operations are explicit and few: {@link BudgetActivityDrawer#clickDone()} (saves an activity),
 * {@link #saveAsDraft()} and {@link #submit()}. Nothing here deletes anything except the unsaved-card removal icon.
 */
public class BudgetCreatePage extends BudgetPageBase {

    public static final int MAX_ACTIVITIES = 25;
    private static final Pattern GROUP_ID_IN_URL = Pattern.compile("/anp/budget/bulk-edit/(\\d+)");

    // ---------------------------------------------------------------- opening

    /** Opens the Create Budget route directly and waits until its Step 1 has rendered. */
    public void open() {
        navigateToUrl(BudgetNavigationPage.baseUrl() + "/anp/budget/bulk-create");
        waitForLoaded();
    }

    /** Navigates to the Create Budget route without waiting for anything, for the page-load checks (D-33). */
    public void navigateOnly() {
        navigateToUrl(BudgetNavigationPage.baseUrl() + "/anp/budget/bulk-create");
    }

    public void refreshPage() {
        refresh();
    }

    /**
     * Waits for Step 1 with the business unit's own hierarchy label. The page first renders with the default "Zone"
     * wording and switches to "Country" once the business unit's configuration has loaded, so "Country" is the signal
     * that the page is ready to use.
     */
    public void waitForLoaded() {
        if (!waitForVisible(Create.COUNTRY_CAPTION, SHELL_TIMEOUT_SECONDS)) {
            reportStep("Create Budget did not finish rendering Step 1 within " + SHELL_TIMEOUT_SECONDS + "s", "FAIL");
        }
    }

    /** Whether Create Budget rendered within the given time; a query for the page-load checks (D-33). */
    public boolean rendersWithin(int seconds) {
        return waitForVisible(Create.COUNTRY_CAPTION, seconds);
    }

    public boolean isDisplayed() {
        return visible(Create.HEADING);
    }

    public String currentUrl() {
        return getCurrentUrl();
    }

    /** The group id in /anp/budget/bulk-edit/{id}, or null on any other page. */
    public String groupIdFromUrl() {
        Matcher m = GROUP_ID_IN_URL.matcher(getCurrentUrl());
        return m.find() ? m.group(1) : null;
    }

    // ---------------------------------------------------------------- header

    public String counterText() {
        return domTextOf(Create.COUNTER_CHIP);
    }

    public String helperText() {
        return domTextOf(Create.HELPER_TEXT);
    }

    public boolean isTextShown(String contains) {
        return visible(Create.text(contains));
    }

    /** Waits for a message to be shown (toasts and dialogs appear asynchronously); false when it never is. */
    public boolean waitForText(String contains, int seconds) {
        return waitUntil(() -> isTextShown(contains), seconds);
    }

    /** Waits until a message is no longer shown; false when it is still there after the time. */
    public boolean waitUntilTextGone(String contains, int seconds) {
        return waitUntil(() -> !isTextShown(contains), seconds);
    }

    /** Whether the editable month input exists (it does on Create, not on Edit Bulk Budget, where the month is a read-only chip). */
    public boolean isMonthInputPresent() {
        return exists(Create.MONTH_INPUT);
    }

    /** Waits for the cards of Edit Bulk Budget to load (that page has no Step 1). */
    public boolean waitForCards(int seconds) {
        return waitUntil(() -> cardCount() > 0, seconds);
    }

    public boolean isLimitReachedShown() {
        return visible(Create.LIMIT_REACHED);
    }

    // ---------------------------------------------------------------- Step 1

    public boolean isCountryFieldDisplayed() {
        return visible(Create.COUNTRY_SELECT);
    }

    public String selectedCountry() {
        return textOf(Create.COUNTRY_SELECT);
    }

    /** The countries offered, read by opening the list and closing it again; the placeholder entries are dropped. */
    public List<String> countryOptions() {
        List<String> options = readSelectOptions(Create.COUNTRY_SELECT);
        options.removeIf(o -> o.isBlank() || o.startsWith("Select"));
        return options;
    }

    public void selectCountry(String country) {
        selectOption(Create.COUNTRY_SELECT, country);
    }

    public boolean isMonthEnabled() {
        return enabled(Create.MONTH_INPUT);
    }

    public String monthInputType() {
        return getAttribute(find(Create.MONTH_INPUT), "type");
    }

    /** The input's min attribute, e.g. "2026-10" (the current month). */
    public String monthMin() {
        String min = getAttribute(find(Create.MONTH_INPUT), "min");
        return min == null ? "" : min;
    }

    /** The month field's value as "yyyy-MM", or "" when empty. */
    public String monthValue() {
        return valueOf(Create.MONTH_INPUT);
    }

    /**
     * Sets the month field to {@code yyyy-MM} in ONE change, exactly as a paste or Playwright's fill does, and does not
     * wait for the app to accept it. Typing key by key cannot do this: a native month input has a month and a year
     * segment, and typing them one after the other produces intermediate values (for example 2026-01 on the way to
     * 2026-11) that an app validating each change would clear. BaseDriver has no wrapper for this, so the value is set with
     * the browser's own value setter (which React-controlled inputs also see) followed by input and change events.
     * Use {@link #setBudgetMonth} when the month must be accepted.
     */
    public void typeBudgetMonth(YearMonth month) {
        setMonthField(month.toString());
    }

    private void setMonthField(String isoMonthOrEmpty) {
        ((JavascriptExecutor) getDriver()).executeScript(
                "const el = arguments[0];"
                        + "Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(el, arguments[1]);"
                        + "el.dispatchEvent(new Event('input', {bubbles: true}));"
                        + "el.dispatchEvent(new Event('change', {bubbles: true}));",
                find(Create.MONTH_INPUT), isoMonthOrEmpty);
        reportStep("The budget month field is set to '" + isoMonthOrEmpty + "'", "INFO");
    }

    /** Types the month and waits until the field holds it; fails the test when the month is not accepted. */
    public void setBudgetMonth(YearMonth month) {
        typeBudgetMonth(month);
        String expected = month.toString();
        if (!waitUntil(() -> expected.equals(monthValue()), UI_TIMEOUT_SECONDS)) {
            reportStep("The budget month " + expected + " was not accepted (the field shows '" + monthValue() + "')", "FAIL");
        }
        waitUntil(() -> isAddActivityEnabled(), UI_TIMEOUT_SECONDS);
    }

    public boolean waitForMonthEquals(YearMonth month, int seconds) {
        String expected = month.toString();
        return waitUntil(() -> expected.equals(monthValue()), seconds);
    }

    /** True once the month field no longer holds the given month (the app cleared or refused it). */
    public boolean waitForMonthNotEqual(YearMonth month, int seconds) {
        String rejected = month.toString();
        return waitUntil(() -> !rejected.equals(monthValue()), seconds);
    }

    /** Whether the app shows a warning, toast or dialog after a month change (waits briefly, since it appears asynchronously). */
    public boolean isMonthChangeWarningShown() {
        return waitUntil(() -> visible(Create.WARNING_SURFACE), 3);
    }

    /** Whether a confirmation dialog (not the activity form or the picker) is open. */
    public boolean isConfirmationShown(int waitSeconds) {
        return waitUntil(() -> visible(Create.CONFIRM_DIALOG), waitSeconds);
    }

    /** Empties the month field. */
    public void clearBudgetMonth() {
        setMonthField("");
    }

    // ---------------------------------------------------------------- Add Activity and the picker

    /** The number of Add Activity buttons that are displayed (0 once the limit is reached). */
    public int addActivityButtonCount() {
        int shown = 0;
        for (WebElement button : getDriver().findElements(xp(Create.ADD_ACTIVITY))) {
            if (button.isDisplayed()) {
                shown++;
            }
        }
        return shown;
    }

    /** Whether the Add Activity buttons are enabled (false when absent, e.g. once the limit is reached). */
    public boolean isAddActivityEnabled() {
        return enabled(Create.ADD_ACTIVITY);
    }

    public void openActivityPicker() {
        click(find(Create.ADD_ACTIVITY));
        mustBeVisible(Picker.DIALOG, "The Add Standard Activity picker");
        mustBeVisible(Picker.SEARCH, "The picker search");
    }

    public boolean isPickerOpen() {
        return visible(Picker.DIALOG);
    }

    public List<String> pickerActivityNames() {
        return textsOf(Picker.ROW_NAMES);
    }

    public void searchPicker(String text) {
        replaceText(find(Picker.SEARCH), text);
    }

    public boolean isNoActivitiesFoundShown() {
        return visible(Picker.NO_RESULTS);
    }

    /** Closes the picker with Escape. */
    public void closePicker() {
        typeWithoutClear(find(Picker.SEARCH), Keys.ESCAPE.toString());
        waitUntil(() -> !isPickerOpen(), UI_TIMEOUT_SECONDS);
    }

    /** Adds an activity from the picker; the app adds an unsaved card and opens its form. */
    public BudgetActivityDrawer addActivity(String activity) {
        openActivityPicker();
        click(find(Picker.addButton(activity)));
        BudgetActivityDrawer drawer = new BudgetActivityDrawer();
        drawer.waitUntilOpen();
        return drawer;
    }

    /**
     * Adds an activity from the picker WITHOUT requiring the activity form to open (on Edit Bulk Budget the app adds an
     * unsaved card instead, D-63). Whether the form opened is for the caller to check.
     */
    public void addActivityFromPicker(String activity) {
        openActivityPicker();
        click(find(Picker.addButton(activity)));
        waitUntil(() -> !isPickerOpen(), UI_TIMEOUT_SECONDS);
    }

    public boolean isActivityFormOpenWithin(int seconds) {
        return waitUntil(() -> new BudgetActivityDrawer().isOpen(), seconds);
    }

    // ---------------------------------------------------------------- cards

    public int cardCount() {
        return count(Create.CARDS);
    }

    public String cardText(int card) {
        return domTextOf(Create.card(card));
    }

    public BudgetActivityDrawer editCard(int card) {
        click(find(Create.cardEdit(card)));
        BudgetActivityDrawer drawer = new BudgetActivityDrawer();
        drawer.waitUntilOpen();
        return drawer;
    }

    public void viewCard(int card) {
        click(find(Create.cardView(card)));
    }

    /** The trash icon of a card: removes an unsaved card at once; for a saved card it opens the delete dialog. */
    public void clickRemoveCard(int card) {
        click(find(Create.cardRemove(card)));
    }

    public boolean isCardChecked(int card) {
        return find(Create.cardCheckbox(card)).isSelected();
    }

    public void toggleCard(int card) {
        click(find(Create.cardCheckbox(card)));
    }

    public boolean isCardSaved(int card) {
        return cardText(card).contains("Saved successfully");
    }

    /** Waits until a card shows "Saved successfully"; false when it does not within the time. */
    public boolean waitForCardSaved(int card, int seconds) {
        return waitUntil(() -> cardCount() >= card && isCardSaved(card), seconds);
    }

    /** The database id printed on a saved card as "#2487", or null. */
    public String activityIdOfCard(int card) {
        Matcher m = Pattern.compile("#(\\d+)").matcher(cardText(card));
        return m.find() ? m.group(1) : null;
    }

    /** Whether the "Delete Activity?" confirmation for a SAVED activity is shown. */
    public boolean isDeleteConfirmationShown() {
        return waitUntil(() -> visible(Create.text("Delete Activity?")), 5);
    }

    /** Declines the delete confirmation ("Keep it"): nothing is deleted. */
    public void clickKeepIt() {
        click(find(Create.KEEP_IT_BUTTON));
        waitUntil(() -> !visible(Create.KEEP_IT_BUTTON), UI_TIMEOUT_SECONDS);
    }

    /** Clicks the confirm button of a submission dialog when there is one; false when no such dialog is shown. */
    public boolean confirmSubmissionIfAsked(int waitSeconds) {
        if (!waitUntil(() -> visible(Create.SUBMIT_CONFIRM_BUTTON), waitSeconds)) {
            return false;
        }
        click(find(Create.SUBMIT_CONFIRM_BUTTON));
        return true;
    }

    /** Waits until the browser has left a route fragment such as "/bulk-edit". */
    public boolean waitUntilUrlNoLonger(String fragment, int seconds) {
        return waitUntil(() -> !getCurrentUrl().contains(fragment), seconds);
    }

    public boolean waitUntilUrlContains(String fragment, int seconds) {
        return waitUntil(() -> getCurrentUrl().contains(fragment), seconds);
    }

    // ---------------------------------------------------------------- footer and totals

    public boolean isSaveAsDraftEnabled() {
        return enabled(Create.SAVE_AS_DRAFT);
    }

    public boolean isSubmitEnabled() {
        return enabled(Create.SUBMIT);
    }

    public boolean isCancelEnabled() {
        return enabled(Create.CANCEL);
    }

    public String saveAsDraftCaption() {
        return domTextOf(Create.SAVE_AS_DRAFT);
    }

    public String submitCaption() {
        return domTextOf(Create.SUBMIT);
    }

    /** E.g. "1 of 2 checked activities ready". */
    public String readyText() {
        return domTextOf(Create.READY_FOOTER);
    }

    public String selectedText() {
        return domTextOf(Create.SELECTED_FOOTER);
    }

    public BigDecimal headerTotalRequested() {
        return parseAmount(domTextOf(Create.HEADER_TOTAL_REQUESTED));
    }

    public BigDecimal activitiesTotal() {
        return parseAmount(domTextOf(Create.ACTIVITIES_TOTAL));
    }

    public BigDecimal grandTotal() {
        return parseAmount(domTextOf(Create.GRAND_TOTAL));
    }

    /**
     * The amounts the Create / Edit page shows at one moment: the header "Total Requested", the "Total" of the Standard
     * Activities section and the footer "Grand Total". An amount the page does not show at that moment is null.
     */
    public record Totals(BigDecimal headerTotalRequested, BigDecimal activitiesTotal, BigDecimal grandTotal, String selected, String ready) {

        /** True when every amount shown is zero (an amount that is not shown counts as zero: nothing is requested). */
        public boolean isZero() {
            return isZero(activitiesTotal) && isZero(grandTotal);
        }

        private static boolean isZero(BigDecimal value) {
            return value == null || value.signum() == 0;
        }

        /** True when the section Total and the Grand Total both equal the amount. */
        public boolean equalsAmount(long amount) {
            return activitiesTotal != null && grandTotal != null
                    && activitiesTotal.compareTo(BigDecimal.valueOf(amount)) == 0 && grandTotal.compareTo(BigDecimal.valueOf(amount)) == 0;
        }

        @Override
        public String toString() {
            return "Total Requested=" + headerTotalRequested + ", Total=" + activitiesTotal + ", Grand Total=" + grandTotal
                    + " [" + selected + "; " + ready + "]";
        }
    }

    /** One reading of the totals. */
    public Totals totals() {
        return new Totals(headerTotalRequested(), activitiesTotal(), grandTotal(), selectedText(), readyText());
    }

    /**
     * The totals once they have stopped changing: two consecutive readings are equal (the page recalculates after a
     * check, an uncheck or a save). Returns the last reading when they never settle within the time.
     */
    public Totals settledTotals(int seconds) {
        Totals[] last = new Totals[1];
        waitUntil(() -> {
            Totals now = totals();
            boolean settled = now.equals(last[0]);
            last[0] = now;
            return settled;
        }, seconds);
        return last[0] != null ? last[0] : totals();
    }

    /** Cancels the page and waits until the Budget list is back. The portal asks for no confirmation (D-69). */
    public void cancel() {
        click(find(Create.CANCEL));
        if (!waitUntil(() -> !getCurrentUrl().contains("/bulk-create") && !getCurrentUrl().contains("/bulk-edit"), UI_TIMEOUT_SECONDS)) {
            reportStep("Cancel did not leave the page within " + UI_TIMEOUT_SECONDS + "s", "FAIL");
        }
    }

    /** WRITES: saves the checked, ready activities as a draft. */
    public void saveAsDraft() {
        click(find(Create.SAVE_AS_DRAFT));
    }

    /** WRITES: submits the checked, ready activities for approval. */
    public void submit() {
        click(find(Create.SUBMIT));
    }

    // ---------------------------------------------------------------- dialogs

    public boolean isDialogTextShown(String text) {
        return visible(Create.ANY_DIALOG + "//*[not(*)][contains(normalize-space(), " + ai.metayb.ui.components.budget.BudgetElements.lit(text) + ")]");
    }

    public boolean isAnyDialogOpen() {
        return visible(Create.ANY_DIALOG);
    }

    public boolean isOkButtonEnabled() {
        return enabled(Create.OK_BUTTON);
    }

    public void clickOk() {
        click(find(Create.OK_BUTTON));
    }
}
