package ai.metayb.ui.pages.budget;

import ai.metayb.ui.components.budget.BudgetElements.ListPage;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The ANP dashboard (/anp/dashboard), only to read its filter counts for the comparison with the Budget list. */
public class BudgetDashboardPage extends BudgetPageBase {

    private static final Pattern NUMBER_IN_BRACKETS = Pattern.compile("\\((\\d[\\d,]*)");

    public void open() {
        navigateToUrl(BudgetNavigationPage.baseUrl() + "/anp/dashboard");
        mustBeVisible(ListPage.filterLabel("Country"), "The dashboard Country filter");
    }

    public boolean hasFilter(String caption) {
        return visible(ListPage.filterLabel(caption));
    }

    /** The count shown inside a dashboard filter, e.g. 7 for "All (7)" / "All Country (7)"; -1 when not shown. */
    public int filterCount(String caption) {
        Matcher m = NUMBER_IN_BRACKETS.matcher(domTextOf(ListPage.filterSelect(caption)));
        return m.find() ? Integer.parseInt(m.group(1).replace(",", "")) : -1;
    }
}
