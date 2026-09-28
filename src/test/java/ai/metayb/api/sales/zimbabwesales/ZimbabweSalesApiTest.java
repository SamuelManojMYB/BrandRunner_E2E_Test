package ai.metayb.api.sales.zimbabwesales;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.BaseApiTest;

import static io.restassured.RestAssured.given;

/**
 * "04. Sales / Zimbabwe Sales" - read requests that return real JSON in this QA tenant
 * (the Zimbabwe-specific module IS enabled/live here, confirmed live). Every "by id"
 * write route in this Postman subfolder returns the CloudFront/S3 SPA fallback - see
 * ZimbabweSalesWriteOperationsApiTest's class Javadoc for the live evidence.
 */
@Epic("BrandRunners Web API")
@Feature("Sales - Zimbabwe Sales")
public class ZimbabweSalesApiTest extends BaseApiTest {

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Team Attendance Calendar with a valid month returns real attendance data")
    @Story("Get Team Attendance Calendar")
    @Description("Verified live: the real required query param is 'month' (YYYY-MM) - 'userId' alone is rejected. " +
            "HTTP 200 for month=2026-09, success=true, data.data is a non-empty array of 13 real sales reps with " +
            "per-day attendance status, data.teamSummary.totalReps='13'.")
    public void getTeamAttendanceCalendarWithValidMonthReturnsData() {
        Response response = given().spec(requestSpecification)
                .queryParam("month", "2026-09").get("/web/zimbabwe-sales/team-attendance-calendar");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertFalse(response.jsonPath().getList("data.data").isEmpty(), "data.data should be non-empty");
        Assert.assertEquals(response.jsonPath().getString("data.teamSummary.totalReps"), "13");
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Get Team Attendance Calendar without required params is rejected")
    @Story("Get Team Attendance Calendar")
    @Description("Verified live: HTTP 400, message 'Invalid input: expected string, received undefined'. Matches " +
            "the Postman collection's own saved example, which sends this request with no query params.")
    public void getTeamAttendanceCalendarWithoutParamsFails() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-sales/team-attendance-calendar");

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(response.jsonPath().getString("message"), "Invalid input: expected string, received undefined");
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Get Candidate Orders without required params is rejected")
    @Story("Get Candidate Orders")
    @Description("Verified live: HTTP 400, message 'Invalid input: expected number, received NaN'. Tried " +
            "locationId/candidateId query params without finding the exact required contract - the Postman " +
            "collection's own saved example carries no query params either, so this is the only scenario that can " +
            "be replayed exactly as Postman defines it. Documented as validation-only.")
    public void getCandidateOrdersWithoutParamsFails() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-sales/candidate-orders");

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(response.jsonPath().getString("message"), "Invalid input: expected number, received NaN");
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Sales Rep Assignments returns the assignments list structure")
    @Story("Get Sales Rep Assignments")
    @Description("Verified live: HTTP 200, success=true, data.data is an array with pagination metadata.")
    public void getSalesRepAssignmentsReturnsStructure() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-sales/sales-rep-assignments");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data.data"));
        Assert.assertNotNull(response.jsonPath().getInt("data.pagination.total"));
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Get Sales Rep Assignments Eligible without required params is rejected")
    @Story("Get Sales Rep Assignments Eligible")
    @Description("Verified live: HTTP 400, message 'Invalid input: expected string, received undefined'. Tried " +
            "locationId/designationId query params without finding the exact required contract - the Postman " +
            "collection's own saved example carries no query params either. Documented as validation-only.")
    public void getSalesRepAssignmentsEligibleWithoutParamsFails() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-sales/sales-rep-assignments/eligible");

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(response.jsonPath().getString("message"), "Invalid input: expected string, received undefined");
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Customers returns the Zimbabwe customer list structure")
    @Story("Get Customers")
    @Description("Verified live: HTTP 200, success=true, data.data is an array with pagination metadata " +
            "(total/totalAll/page/limit/totalPages/sortBy/sortOrder).")
    public void getCustomersReturnsStructure() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-sales/customers");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data.data"));
        Assert.assertNotNull(response.jsonPath().getInt("data.total"));
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Get Customers without authentication fails")
    @Story("Get Customers")
    @Description("Verified live: HTTP 401, message 'Authentication token missing'.")
    public void getCustomersWithoutAuthFails() {
        Response response = given().spec(noAuthRequestSpecification).get("/web/zimbabwe-sales/customers");

        Assert.assertEquals(response.statusCode(), 401);
        Assert.assertEquals(response.jsonPath().getString("message"), "Authentication token missing");
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Customers Lookup with a 2+ character query returns the lookup structure")
    @Story("Get Customers Lookup")
    @Description("Verified live for q='te': HTTP 200, success=true, data is an array (empty in this tenant).")
    public void getCustomersLookupWithValidQueryReturnsStructure() {
        Response response = given().spec(requestSpecification).queryParam("q", "te").get("/web/zimbabwe-sales/customers/lookup");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data"));
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Get Customers Lookup with a query shorter than 2 characters is rejected")
    @Story("Get Customers Lookup")
    @Description("Verified live: HTTP 400, message 'q must be at least 2 characters'.")
    public void getCustomersLookupWithTooShortQueryFails() {
        Response response = given().spec(requestSpecification).queryParam("q", "t").get("/web/zimbabwe-sales/customers/lookup");

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(response.jsonPath().getString("message"), "q must be at least 2 characters");
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Customers Bulk Template returns a downloadable spreadsheet template")
    @Story("Get Customers Bulk Template")
    @Description("Verified live: HTTP 200, response body is a real binary spreadsheet (ZIP-format signature 'PK'), " +
            "not a JSON success envelope like every other endpoint in this folder - this one streams the file directly.")
    public void getCustomersBulkTemplateReturnsSpreadsheet() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-sales/customers/bulk/template");

        Assert.assertEquals(response.statusCode(), 200);
        byte[] body = response.getBody().asByteArray();
        Assert.assertTrue(body.length > 0, "response body should be non-empty");
        Assert.assertEquals(body[0], (byte) 'P', "spreadsheet body should start with the ZIP signature 'PK'");
        Assert.assertEquals(body[1], (byte) 'K', "spreadsheet body should start with the ZIP signature 'PK'");
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Customers Checklists returns the onboarding checklist structure")
    @Story("Get Customers Checklists")
    @Description("Verified live: HTTP 200, success=true, data is an array (empty in this tenant).")
    public void getCustomersChecklistsReturnsStructure() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-sales/customers/checklists");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data"));
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Customers Outlets returns the outlet type structure")
    @Story("Get Customers Outlets")
    @Description("Verified live: HTTP 200, success=true, data is an array (empty in this tenant).")
    public void getCustomersOutletsReturnsStructure() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-sales/customers/outlets");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data"));
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Customers Types returns the customer type structure")
    @Story("Get Customers Types")
    @Description("Verified live: HTTP 200, success=true, data is an array (empty in this tenant).")
    public void getCustomersTypesReturnsStructure() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-sales/customers/types");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data"));
    }
}
