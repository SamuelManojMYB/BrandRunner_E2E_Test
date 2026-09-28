package ai.metayb.api.sales.zimbabwesales;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.annotations.Test;
import testUtils.BaseApiTest;

/**
 * "04. Sales / Zimbabwe Sales" - every write-verb request, plus every GET request whose
 * path resolves to a specific existing/would-be record (by customerId/addressId/logId/
 * saleId/id), covering 33 Postman requests across 25 distinct routes.
 *
 * NOT AUTOMATED in this QA environment - same systemic CloudFront/S3 SPA-fallback root
 * cause already documented across every other folder (e.g.
 * LocationWriteOperationsApiTest, SalesOrdersWriteOperationsApiTest). Verified live for
 * every route below individually with realistic bodies and safe non-existent ids
 * (999999, or 1 where a real customer/attendance-day id was needed) - every call
 * returned HTTP 200 with an HTML SPA document instead of JSON.
 *
 * One further request in this Postman subfolder, "Get Team Attendance Leave Requests"
 * (GET, no id), is NOT part of this class - it fails for a genuinely different reason
 * (the route itself doesn't exist on the backend: HTTP 400 "Can't find
 * /web/zimbabwe-sales/team-attendance/leave-requests on this server!!!", not the SPA
 * fallback) - see its own placeholder test below with a distinct explanation.
 */
@Epic("BrandRunners Web API")
@Feature("Sales - Zimbabwe Sales")
public class ZimbabweSalesWriteOperationsApiTest extends BaseApiTest {

    @Test(enabled = false, groups = {"api", "regression"},
            description = "NOT IMPLEMENTED: GET /web/zimbabwe-sales/team-attendance/leave-requests does not exist on this backend")
    @Story("Get Team Attendance Leave Requests")
    @Description("Verified live: HTTP 400, message \"Can't find /web/zimbabwe-sales/team-attendance/leave-requests " +
            "on this server!!!\" - a genuine backend route-not-found response (distinct from the CloudFront/S3 SPA " +
            "fallback affecting every other blocked request in this class), confirming this specific endpoint was " +
            "never implemented on the backend, not merely blocked by environment. Not executed.")
    public void getTeamAttendanceLeaveRequestsNotImplementedOnBackend() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: POST /web/zimbabwe-sales/team-attendance/leave-requests/:attendanceDayId/approve returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Approve Team Attendance Leave Requests by Attendance Day ID")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void approveTeamAttendanceLeaveRequestNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: POST /web/zimbabwe-sales/team-attendance/leave-requests/:attendanceDayId/reject returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Reject Team Attendance Leave Requests by Attendance Day ID")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void rejectTeamAttendanceLeaveRequestNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: POST /web/zimbabwe-sales/allocations/create returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Create Allocations")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void createAllocationsNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: GET /web/zimbabwe-sales/sales-rep-assignments/:id returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Get Sales Rep Assignments by ID")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void getSalesRepAssignmentsByIdNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: POST /web/zimbabwe-sales/sales-rep-assignments returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Sales Rep Assignments")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void createSalesRepAssignmentNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: PUT /web/zimbabwe-sales/sales-rep-assignments/:id returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Update Sales Rep Assignments by ID")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void updateSalesRepAssignmentByIdNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: DELETE /web/zimbabwe-sales/sales-rep-assignments/:id returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Delete Sales Rep Assignments by ID")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void deleteSalesRepAssignmentByIdNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: POST /web/zimbabwe-sales/reconciliation/initiate returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Reconciliation Initiate")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void reconciliationInitiateNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: POST /web/zimbabwe-sales/customers returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Customers")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void createCustomerNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: POST /web/zimbabwe-sales/customers/bulk/upload returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Upload Customers Bulk")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void uploadCustomersBulkNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: POST/PATCH/DELETE /web/zimbabwe-sales/customers/checklists(/:checklistId) returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Customers Checklists / Update Customers Checklists / Delete Customers Checklists")
    @Description("Blocked by environment: see class Javadoc. Covers all 3 write requests for this sub-resource " +
            "(create/update/delete) - same route family, same result. Not executed.")
    public void customersChecklistsWriteNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: POST/PATCH/DELETE /web/zimbabwe-sales/customers/outlets(/:outletId) returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Customers Outlets / Update Customers Outlets / Delete Customers Outlets")
    @Description("Blocked by environment: see class Javadoc. Covers all 3 write requests for this sub-resource " +
            "(create/update/delete) - same route family, same result. Not executed.")
    public void customersOutletsWriteNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: POST/PATCH/DELETE /web/zimbabwe-sales/customers/types(/:customerTypeId) returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Customers Types / Update Customers Types by Customer Type ID / Delete Customers Types by Customer Type ID")
    @Description("Blocked by environment: see class Javadoc. Covers all 3 write requests for this sub-resource " +
            "(create/update/delete) - same route family, same result. Not executed.")
    public void customersTypesWriteNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: GET /web/zimbabwe-sales/customers/:customerId returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Get Customers (by ID)")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void getCustomerByIdNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: GET /web/zimbabwe-sales/customers/:customerId/activity-logs returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Get Customers Activity Logs")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void getCustomerActivityLogsNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: POST /web/zimbabwe-sales/customers/:customerId/pending-edits/:logId/approve|reject returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Approve Customers Pending Edits by Log ID / Reject Customers Pending Edits by Log ID")
    @Description("Blocked by environment: see class Javadoc. Covers both approve and reject variants - same route " +
            "family, same result. Not executed.")
    public void customerPendingEditsApproveRejectNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: PATCH /web/zimbabwe-sales/customers/:customerId returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Update Customers")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void updateCustomerNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: PATCH /web/zimbabwe-sales/customers/:customerId/status returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Update Customers Status")
    @Description("Blocked by environment: see class Javadoc. Not executed.")
    public void updateCustomerStatusNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: POST/PATCH /web/zimbabwe-sales/customers/:customerId/addresses(/:addressId)(/status) returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Customers Addresses / Update Customers Addresses / Update Customers Addresses Status")
    @Description("Blocked by environment: see class Javadoc. Covers all 3 write requests for this sub-resource " +
            "(create/update/update-status) - same route family, same result. Not executed.")
    public void customerAddressesWriteNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: GET /web/zimbabwe-sales/customers/:customerId/addresses(/:addressId) returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Get Customers Addresses")
    @Description("Blocked by environment: see class Javadoc. Covers both the list and by-addressId Postman " +
            "variants of this request - same route family, same result. Not executed.")
    public void getCustomerAddressesNotAutomatedDueToEnvironmentIssue() {
    }

    @Test(enabled = false, groups = {"api", "regression"},
            description = "BLOCKED: PATCH /web/zimbabwe-sales/:saleId/confirm|recreate|reject returns the CloudFront/S3 SPA fallback, not JSON")
    @Story("Update Confirm by Sale ID / Update Recreate by Sale ID / Reject by Sale ID")
    @Description("Blocked by environment: see class Javadoc. Covers all 3 sale-state-transition requests - same " +
            "route family, same result. Not executed.")
    public void saleConfirmRecreateRejectNotAutomatedDueToEnvironmentIssue() {
    }
}
