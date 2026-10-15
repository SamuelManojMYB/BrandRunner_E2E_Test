package ai.metayb.ui.budget;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Test data of the Budget Creation / Budget Alerts document (section 1, "Common Test Data - Yeppie"). Nothing here is a
 * credential; the sign-in comes from BUDGET_UI_USERNAME / BUDGET_UI_PASSWORD through ConfigManager.
 */
public final class BudgetTestData {

    private BudgetTestData() {
    }

    public static final String BUSINESS_UNIT = "Yeppie";
    public static final String CURRENCY = "₦";   // naira sign

    // location: Country > State > County > City
    public static final String COUNTRY = "United States";
    public static final String STATE = "California";
    public static final String COUNTY = "San Francisco County";
    public static final String CITY = "San Francisco";
    public static final String OTHER_COUNTY = "Los Angeles County";

    public static final String ACTIVITY = "Event Sampling";

    public static final String PRODUCT = "ADDME CHICKEN FLAVOUR 25X10ROLLS X 8G";
    public static final int PRODUCT_RATE = 100;

    public static final String DESIGNATION = "Adityapromoter";
    public static final int DESIGNATION_RATE = 500;
    public static final String OTHER_DESIGNATION = "Aditya Supervisor";
    public static final int OTHER_DESIGNATION_RATE = 1000;

    public static final int MAX_ACTIVITIES = 25;

    /** The budget month the tests use: next month, so it is never rejected as a past month (the current month is the minimum). */
    public static YearMonth testMonth() {
        return YearMonth.now().plusMonths(1);
    }

    public static YearMonth monthAfter(YearMonth month, int months) {
        return month.plusMonths(months);
    }

    /** "November 2026", as the app prints it. */
    public static String monthLabel(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + month.getYear();
    }

    /** The nearest February after the current month, within a year (the document tests 28-day February handling). */
    public static YearMonth nextFebruary() {
        YearMonth m = YearMonth.now().plusMonths(1);
        while (m.getMonthValue() != 2) {
            m = m.plusMonths(1);
        }
        return m;
    }

    public static YearMonth pastMonth() {
        return YearMonth.now().minusMonths(1);
    }

    /** QA-Claude-Yeppie-&lt;testId&gt;-&lt;timestamp&gt;, the collision-free title the document recommends. */
    public static String uniqueTitle(String testId) {
        return "QA-Claude-Yeppie-" + testId + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }
}
