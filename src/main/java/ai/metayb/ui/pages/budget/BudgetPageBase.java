package ai.metayb.ui.pages.budget;

import ai.metayb.ui.components.budget.BudgetElements;
import ai.metayb.ui.wrappers.BaseDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Shared plumbing of the Budget page objects, built on the existing BaseDriver wrappers.
 *
 * Two kinds of waits are used on purpose. The BaseDriver waits (waitForElementVisible, ...) FAIL the test on timeout,
 * which is right for things that must appear. {@link #waitUntil} returns false instead, for checks where the
 * ABSENCE of something is the expected result (e.g. a message that must not appear): such checks never retry, so they
 * cannot hide a defect.
 */
public abstract class BudgetPageBase extends BaseDriver {

    protected static final int SHELL_TIMEOUT_SECONDS = 30;
    protected static final int UI_TIMEOUT_SECONDS = 20;

    // ---------------------------------------------------------------- locating (quiet queries)

    protected By xp(String xpath) {
        return By.xpath(xpath);
    }

    protected WebElement find(String xpath) {
        return locateElement("xpath", xpath);
    }

    /** Whether at least one element matches. A query: an absent element is a result, not a failure, and nothing is logged. */
    protected boolean exists(String xpath) {
        return !getDriver().findElements(By.xpath(xpath)).isEmpty();
    }

    protected boolean visible(String xpath) {
        try {
            List<WebElement> found = getDriver().findElements(By.xpath(xpath));
            for (WebElement element : found) {
                if (element.isDisplayed()) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    protected int count(String xpath) {
        return getDriver().findElements(By.xpath(xpath)).size();
    }

    protected boolean enabled(String xpath) {
        try {
            List<WebElement> found = getDriver().findElements(By.xpath(xpath));
            return !found.isEmpty() && found.get(0).isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    /** The visible text of the first match, trimmed and with runs of white space collapsed. */
    protected String textOf(String xpath) {
        return getText(find(xpath)).replaceAll("\\s+", " ").trim();
    }

    /** The DOM text content (independent of CSS such as text-transform) of the first match, or "" when absent. */
    protected String domTextOf(String xpath) {
        List<WebElement> found = getDriver().findElements(By.xpath(xpath));
        if (found.isEmpty()) {
            return "";
        }
        String text = found.get(0).getDomProperty("textContent");
        return text == null ? "" : text.replaceAll("\\s+", " ").trim();
    }

    /**
     * The texts of all matches. A displayed element is read as the user sees it (its parts separated by white space, e.g.
     * a product name and its price in two elements); a hidden one falls back to its DOM text.
     */
    protected List<String> textsOf(String xpath) {
        List<String> texts = new ArrayList<>();
        for (WebElement e : getDriver().findElements(By.xpath(xpath))) {
            String t = e.isDisplayed() ? e.getText() : e.getDomProperty("textContent");
            texts.add(t == null ? "" : t.replaceAll("\\s+", " ").trim());
        }
        return texts;
    }

    protected String valueOf(String xpath) {
        String value = find(xpath).getDomProperty("value");
        return value == null ? "" : value;
    }

    // ---------------------------------------------------------------- waiting

    /** Polls a condition; true as soon as it holds, false on timeout. Never fails the test by itself. */
    protected boolean waitUntil(BooleanSupplier condition, int timeoutSeconds) {
        try {
            new WebDriverWait(getDriver(), Duration.ofSeconds(timeoutSeconds)).until(d -> {
                try {
                    return condition.getAsBoolean();
                } catch (Exception e) {
                    return false;
                }
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    protected boolean waitForVisible(String xpath, int timeoutSeconds) {
        return waitUntil(() -> visible(xpath), timeoutSeconds);
    }

    protected boolean waitForGone(String xpath, int timeoutSeconds) {
        return waitUntil(() -> !visible(xpath), timeoutSeconds);
    }

    /** Fails the test (with a screenshot) unless the element becomes visible. */
    protected void mustBeVisible(String xpath, String what) {
        if (!waitForVisible(xpath, UI_TIMEOUT_SECONDS)) {
            reportStep(what + " did not become visible within " + UI_TIMEOUT_SECONDS + "s", "FAIL");
        }
    }

    // ---------------------------------------------------------------- MUI Select

    /** Opens a MUI Select (a click) and waits for its option list. */
    protected void openSelect(String selectXpath) {
        click(find(selectXpath));
        mustBeVisible(BudgetElements.Popup.LISTBOX, "The option list");
    }

    protected List<String> openOptionTexts() {
        return textsOf(BudgetElements.Popup.OPTIONS);
    }

    /** Closes an open option list with Escape (sent to the list, which is what has focus). */
    protected void closeOpenSelect() {
        List<WebElement> lists = getDriver().findElements(By.xpath(BudgetElements.Popup.LISTBOX));
        if (!lists.isEmpty()) {
            typeWithoutClear(lists.get(0), Keys.ESCAPE.toString());
            waitUntil(() -> !visible(BudgetElements.Popup.LISTBOX), 5);
        }
    }

    protected void selectOption(String selectXpath, String optionText) {
        openSelect(selectXpath);
        click(find(BudgetElements.Popup.option(optionText)));
        waitUntil(() -> !visible(BudgetElements.Popup.LISTBOX), UI_TIMEOUT_SECONDS);
    }

    /** For options that carry a suffix such as a price ("Adityapromoter ₦500"). */
    protected void selectOptionStartingWith(String selectXpath, String prefix) {
        openSelect(selectXpath);
        click(find(BudgetElements.Popup.optionStartingWith(prefix)));
        waitUntil(() -> !visible(BudgetElements.Popup.LISTBOX), UI_TIMEOUT_SECONDS);
    }

    /** The options of a Select, read by opening it and closing it again without choosing. */
    protected List<String> readSelectOptions(String selectXpath) {
        openSelect(selectXpath);
        List<String> options = openOptionTexts();
        closeOpenSelect();
        return options;
    }

    // ---------------------------------------------------------------- amounts

    /** "₦ 3,000.00" / "₦3,000" -> 3000.00; null when the text holds no number. */
    public static BigDecimal parseAmount(String text) {
        if (text == null) {
            return null;
        }
        String digits = text.replaceAll("[^0-9.\\-]", "");
        if (digits.isEmpty() || digits.equals("-") || digits.equals(".")) {
            return null;
        }
        return new BigDecimal(digits);
    }
}
