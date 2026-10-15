package ai.metayb.ui.budget;

import ai.metayb.ui.pages.budget.BudgetCreatePage;
import ai.metayb.ui.pages.budget.BudgetListPage;
import ai.metayb.ui.pages.budget.BudgetNavigationPage;
import io.qameta.allure.*;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;
import testUtils.BudgetAuthenticatedTest;

/**
 * E2E-01 and the access / navigation cases of the Budget Creation test document (section 3): Yeppie business unit,
 * opening the Budget module and Create Budget, the initial locked state, and the page-load checks. Strictly read-only:
 * nothing here saves anything.
 */
@Epic("Budget")
@Feature("Budget - Navigation and initial setup")
public class BudgetNavigationTest extends BudgetAuthenticatedTest {

    @Test(groups = {"budget", "budget-readonly", "budget-smoke"},
            description = "E2E-01 (BC-020, BC-021, BC-036, BC-043, AL-001): open Budget as Yeppie, open Create Budget, check the initial locked state, cancel")
    @Story("E2E-01 Navigation and initial setup")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Switches to Yeppie, opens Budget from the side menu, opens Create Budget, verifies the header, Step 1 and the "
            + "locked initial state with its hints, then cancels. Nothing is saved.")
    public void e2e01NavigationAndInitialSetup() {
        BudgetNavigationPage nav = signInAndSelectYeppie();

        // BC-020: the business unit is Yeppie and the app shows its currency
        Assert.assertEquals(nav.currentBusinessUnit(), BudgetTestData.BUSINESS_UNIT, "Active business unit");
        Assert.assertTrue(nav.isBudgetMenuDisplayed(), "The side menu should offer Budget");
        nav.openBudgetFromMenu();
        BudgetListPage list = new BudgetListPage();
        list.waitForLoaded();
        Assert.assertTrue(list.hasCurrencySymbol(), "The Yeppie list should show amounts in " + BudgetTestData.CURRENCY);
        Assert.assertTrue(list.getTotalCount() > 0, "The list should show a total count");

        // BC-021: Create Budget opens /anp/budget/bulk-create with Step 1
        Assert.assertTrue(list.hasCreateBudget(), "The list should offer Create Budget");
        list.clickCreateBudget();
        BudgetCreatePage create = new BudgetCreatePage();
        create.waitForLoaded();
        SoftAssert soft = new SoftAssert();
        soft.assertTrue(create.currentUrl().endsWith("/anp/budget/bulk-create"), "Route should be /anp/budget/bulk-create but is " + create.currentUrl());
        soft.assertEquals(create.counterText(), "0/25", "Header counter");
        soft.assertTrue(create.isDisplayed(), "The 'Create Budget' heading should be shown");
        soft.assertTrue(create.helperText().contains("Select Country") && create.helperText().contains("Max 25 activities"),
                "Helper text should describe the flow and the 25-activity limit but is: " + create.helperText());
        soft.assertTrue(create.isCountryFieldDisplayed(), "Step 1 should show the Country field");

        // BC-036: controls are locked until a Country is chosen; Cancel stays enabled
        soft.assertFalse(create.isMonthEnabled(), "Budget Month should be disabled before a Country is selected");
        soft.assertFalse(create.isAddActivityEnabled(), "Add Activity should be disabled before a Country is selected");
        soft.assertFalse(create.isSaveAsDraftEnabled(), "Save as Draft should be disabled");
        soft.assertFalse(create.isSubmitEnabled(), "Submit should be disabled");
        soft.assertTrue(create.isCancelEnabled(), "Cancel should be enabled");

        // AL-001: the country hints
        soft.assertTrue(create.isTextShown("Select a Country to unlock Budget Month."), "AL-001 hint 'Select a Country to unlock Budget Month.'");
        soft.assertTrue(create.isTextShown("Select Country first"), "AL-001 hint '① Select Country first'");
        soft.assertAll();

        // BC-043: Cancel with no changes returns to the list and creates nothing
        create.cancel();
        list.waitForLoaded();
        Assert.assertTrue(list.isDisplayed(), "Cancel should return to the Budget list");
    }

    @Test(groups = {"budget", "budget-readonly", "budget-defect"},
            description = "BC-023, BC-109 (D-33): Create Budget renders within 5 s on a direct load and on refresh")
    @Story("Page load stability")
    @Severity(SeverityLevel.CRITICAL)
    @Description("Known defect D-33: direct loads and refreshes intermittently stay on a blank loader for more than 45 s. "
            + "The expectation is a render within 5 s. There is no retry or reload, so an occurrence fails the test.")
    public void bc023Bc109D33CreateBudgetRendersWithinFiveSeconds() {
        signInAndSelectYeppie();
        BudgetCreatePage create = new BudgetCreatePage();
        create.navigateOnly();
        Assert.assertTrue(create.rendersWithin(5), "D-33: Create Budget did not render within 5 s on a direct load");
        create.refreshPage();
        Assert.assertTrue(create.rendersWithin(5), "D-33: Create Budget did not render within 5 s after a refresh");
        create.cancel();
    }

    @Test(groups = {"budget", "budget-blocked"}, description = "BC-022: Create Budget is hidden for a user without CREATE_BUDGET")
    @Story("Permissions")
    public void bc022CreateBudgetHiddenWithoutPermission() {
        throw new SkipException("BC-022 needs a second account without the CREATE_BUDGET permission (Switch Profile). The document names "
                + "no such account and the framework has no credentials for one, so it is not automated.");
    }

    @Test(groups = {"budget", "budget-blocked"}, description = "BC-110: opening the side menu must not switch the business unit")
    @Story("Usability")
    public void bc110BusinessUnitDropdownCloseToMenuItems() {
        throw new SkipException("BC-110 describes a click landing 'near the menu header' and switching the unit by accident; the document gives no "
                + "coordinates or steps that can be reproduced deterministically, and coordinate clicks are not allowed.");
    }
}
