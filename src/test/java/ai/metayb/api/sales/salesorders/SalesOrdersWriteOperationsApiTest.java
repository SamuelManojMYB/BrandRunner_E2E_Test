package ai.metayb.api.sales.salesorders;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.annotations.Test;
import testUtils.BaseApiTest;

/**
 * "04. Sales / Sales Orders" - every write-verb request, plus every GET request whose
 * path resolves to a specific existing/would-be record (by allocationId/customerId/id),
 * covering 20 Postman requests across 18 distinct routes.
 *
 * NOT AUTOMATED in this QA environment - same systemic CloudFront/S3 SPA-fallback root
 * cause already documented across every other folder (e.g.
 * LocationWriteOperationsApiTest). Verified live for every route below individually
 * with realistic bodies and safe non-existent ids (999999, or 1 where a real allocation/
 * customer id was needed) - every call returned HTTP 200 with an HTML SPA document
 * (starting "&lt;!DOCTYPE html&gt;") instead of JSON, both for genuine write verbs
 * (POST/PUT/PATCH/DELETE) and for these specific GET-by-id routes.
 */
@Epic("BrandRunners Web API")
@Feature("Sales - Sales Orders")
public class SalesOrdersWriteOperationsApiTest extends BaseApiTest {

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: POST /web/sales/customers/by-locations returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Customers By Locations")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void customersByLocationsNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: GET /web/sales/customers/:customerId/addresses returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Get Customers Addresses")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void getCustomersAddressesNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: GET /web/sales/allocations-by-distributors/:allocationId returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Get Allocations By Distributors (by ID)")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void getAllocationsByDistributorsByIdNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: POST /web/sales/order-allocation returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Order Allocation")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void orderAllocationNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: GET /web/sales/allocations/:allocationId/reconcile-qty-details returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Get Allocations Reconcile Qty Details")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void getAllocationsReconcileQtyDetailsNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: POST /web/sales/allocations/:allocationId/reconcile/approve returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Approve Allocations Reconcile")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void approveAllocationsReconcileNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: PATCH /web/sales/notifications/:userId/read returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Update Notifications Read by User ID")
    @Description("Blocked by environment: see class Javadoc. Not executed. Covers both Postman variants of this " +
            "request (with and without a notificationId path segment) - the same route family, same result.")
    public void updateNotificationsReadNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: GET /web/sales/contingency-pickup-requests/:id returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Get Contingency Pickup Requests by ID")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void getContingencyPickupRequestsByIdNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: PUT /web/sales/contingency-pickup-requests/:id/approve-reject returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Reject Contingency Pickup Requests Approve by ID")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void rejectContingencyPickupRequestsApproveNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: POST /web/sales/create returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Create")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void createNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: PUT /web/sales/update/:id returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Update by ID")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void updateByIdNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: PUT /web/sales/approve/:id returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Approve by ID")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void approveByIdNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: PUT /web/sales/reject/:id returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Reject by ID")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void rejectByIdNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: DELETE /web/sales/delete/:id returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Delete by ID")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void deleteByIdNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: POST /web/sales/failed-to-deliver returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Failed To Deliver")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void failedToDeliverNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: GET /web/sales/:id/activity-logs returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Get Activity Logs by ID")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void getActivityLogsByIdNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: GET /web/sales/:id/view returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Get View by ID")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void getViewByIdNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: GET /web/sales/:id/edit returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Get Edit by ID")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void getEditByIdNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: GET /web/sales/:id returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Get Sales Order by ID")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void getSalesOrderByIdNotAutomatedDueToEnvironmentIssue() {
    }
}
