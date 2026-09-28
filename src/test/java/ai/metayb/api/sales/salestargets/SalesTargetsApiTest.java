package ai.metayb.api.sales.salestargets;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.annotations.Test;
import testUtils.BaseApiTest;

/**
 * "04. Sales / Sales Targets" - all 16 Postman requests, covering 11 distinct routes,
 * ALL under the {{baseUrl}}/web/zimbabwe-sales/target* path family.
 *
 * NOT AUTOMATED - genuinely NOT IMPLEMENTED on this backend, not an environment/
 * CloudFront issue: every route below was verified live and returns HTTP 400 with the
 * body {"success":false,"message":"Can't find &lt;path&gt; on this server!!!"} - a real
 * backend "route not found" response (distinct from the CloudFront/S3 SPA fallback
 * documented in SalesOrdersWriteOperationsApiTest/ZimbabweSalesWriteOperationsApiTest,
 * which returns HTML with a 200 status instead). This is a genuine backend defect/gap:
 * the entire Sales Targets feature area does not exist on the API yet in this
 * environment, for either read or write requests.
 *
 * Directly verified live: "Targets"/"Get Targets" (/web/zimbabwe-sales/targets),
 * "Get Target Products", "Get Targets by ID" (/targets/1), "Target Assignments Bulk",
 * "Get Target Assignments", "Export Target Assignments", "Get Target Report",
 * "Get Target Leaderboard", "Get Target Alerts". "Update/Delete Targets by ID" and
 * "Update/Delete Target Assignments by ID" were not separately called (their base
 * routes /targets and /target-assignments were already confirmed not to exist, so a
 * sub-path under a non-existent base route cannot exist either), nor was
 * "Target Assignments Clone by ID" or "Export Target Report" - both share the exact
 * same not-implemented route family as their siblings above.
 */
@Epic("BrandRunners Web API")
@Feature("Sales - Sales Targets")
public class SalesTargetsApiTest extends BaseApiTest {

    @Test(enabled = false, groups = {"api", "regression"},
            description = "NOT IMPLEMENTED: POST/GET /web/zimbabwe-sales/targets does not exist on this backend")
    @Story("Targets / Get Targets")
    @Description("Backend defect - see class Javadoc. Not executed.")
    public void targetsNotImplementedOnBackend() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "NOT IMPLEMENTED: GET /web/zimbabwe-sales/target-products does not exist on this backend")
    @Story("Get Target Products")
    @Description("Backend defect - see class Javadoc. Not executed.")
    public void getTargetProductsNotImplementedOnBackend() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "NOT IMPLEMENTED: GET/PUT/DELETE /web/zimbabwe-sales/targets/:id does not exist on this backend")
    @Story("Get Targets by ID / Update Targets by ID / Delete Targets by ID")
    @Description("Backend defect - see class Javadoc. Not executed.")
    public void targetByIdNotImplementedOnBackend() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "NOT IMPLEMENTED: POST /web/zimbabwe-sales/target-assignments/bulk does not exist on this backend")
    @Story("Target Assignments Bulk")
    @Description("Backend defect - see class Javadoc. Not executed.")
    public void targetAssignmentsBulkNotImplementedOnBackend() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "NOT IMPLEMENTED: GET /web/zimbabwe-sales/target-assignments does not exist on this backend")
    @Story("Get Target Assignments")
    @Description("Backend defect - see class Javadoc. Not executed.")
    public void getTargetAssignmentsNotImplementedOnBackend() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "NOT IMPLEMENTED: GET /web/zimbabwe-sales/target-assignments/export does not exist on this backend")
    @Story("Export Target Assignments")
    @Description("Backend defect - see class Javadoc. Not executed.")
    public void exportTargetAssignmentsNotImplementedOnBackend() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "NOT IMPLEMENTED: PUT/DELETE /web/zimbabwe-sales/target-assignments/:id does not exist on this backend")
    @Story("Update Target Assignments by ID / Delete Target Assignments by ID")
    @Description("Backend defect - see class Javadoc. Not executed.")
    public void targetAssignmentByIdNotImplementedOnBackend() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "NOT IMPLEMENTED: POST /web/zimbabwe-sales/target-assignments/:id/clone does not exist on this backend")
    @Story("Target Assignments Clone by ID")
    @Description("Backend defect - see class Javadoc. Not executed.")
    public void targetAssignmentsCloneNotImplementedOnBackend() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "NOT IMPLEMENTED: GET /web/zimbabwe-sales/target-report(/export) does not exist on this backend")
    @Story("Get Target Report / Export Target Report")
    @Description("Backend defect - see class Javadoc. Not executed.")
    public void targetReportNotImplementedOnBackend() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "NOT IMPLEMENTED: GET /web/zimbabwe-sales/target-leaderboard does not exist on this backend")
    @Story("Get Target Leaderboard")
    @Description("Backend defect - see class Javadoc. Not executed.")
    public void getTargetLeaderboardNotImplementedOnBackend() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "NOT IMPLEMENTED: GET /web/zimbabwe-sales/target-alerts does not exist on this backend")
    @Story("Get Target Alerts")
    @Description("Backend defect - see class Javadoc. Not executed.")
    public void getTargetAlertsNotImplementedOnBackend() {
    }
}
