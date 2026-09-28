package ai.metayb.api.sales.zimbabwedashboard;

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
 * "04. Sales / Zimbabwe Dashboard" - all 4 Postman requests, all GET, all working in
 * this tenant (this country-specific module IS enabled/live here, confirmed live -
 * not a "wrong tenant" situation).
 */
@Epic("BrandRunners Web API")
@Feature("Sales - Zimbabwe Dashboard")
public class ZimbabweDashboardApiTest extends BaseApiTest {

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Filters Outlets returns the real location filter tree")
    @Story("Get Filters Outlets")
    @Description("Verified live: HTTP 200, success=true, data is a non-empty array of {id,name,level,code} location entries.")
    public void getFiltersOutletsReturnsLocationTree() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-dashboard/filters/outlets");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertFalse(response.jsonPath().getList("data").isEmpty(), "data should be non-empty");
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Get Filters Outlets without authentication fails")
    @Story("Get Filters Outlets")
    @Description("Verified live: HTTP 401, message 'Authentication token missing'.")
    public void getFiltersOutletsWithoutAuthFails() {
        Response response = given().spec(noAuthRequestSpecification).get("/web/zimbabwe-dashboard/filters/outlets");

        Assert.assertEquals(response.statusCode(), 401);
        Assert.assertEquals(response.jsonPath().getString("message"), "Authentication token missing");
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Summary returns the full dashboard KPI structure")
    @Story("Get Summary")
    @Description("Verified live: HTTP 200, success=true, data.filters.dateFrom/dateTo present, data.kpis is a " +
            "non-empty object with real KPI keys (totalCalls, totalSales, etc.), data.deliveryStatus.items is a " +
            "non-empty array. All KPI values are 0 in this tenant (no live sales today) - a valid empty-day state.")
    public void getSummaryReturnsKpiStructure() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-dashboard/summary");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getString("data.filters.dateFrom"));
        Assert.assertNotNull(response.jsonPath().getMap("data.kpis"));
        Assert.assertFalse(response.jsonPath().getList("data.deliveryStatus.items").isEmpty());
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Export Summary returns a base64-encoded export file")
    @Story("Export Summary")
    @Description("Verified live: HTTP 200, success=true, data.fileBase64 is present and non-blank.")
    public void exportSummaryReturnsFile() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-dashboard/summary/export");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        String fileBase64 = response.jsonPath().getString("data.fileBase64");
        Assert.assertNotNull(fileBase64);
        Assert.assertFalse(fileBase64.isBlank());
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Details by Section returns section-specific drill-down data for a valid section")
    @Story("Get Details by Section")
    @Description("Verified live for section='skuPerformance' (one of 7 valid enum values confirmed by the real " +
            "validation error): HTTP 200, success=true, data.section='skuPerformance', data.rows is present.")
    public void getDetailsBySectionWithValidSectionReturnsData() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-dashboard/details/skuPerformance");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertEquals(response.jsonPath().getString("data.section"), "skuPerformance");
        Assert.assertNotNull(response.jsonPath().getList("data.rows"));
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Get Details by Section with an invalid section is rejected")
    @Story("Get Details by Section")
    @Description("Verified live: HTTP 400, message lists the real valid enum: skuPerformance|customerOrders|" +
            "preSalesOrders|restockingSummary|reconciliationSummary|outletSegmentation|outletTypes.")
    public void getDetailsBySectionWithInvalidSectionFails() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-dashboard/details/invalidSectionXyz");

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertTrue(response.jsonPath().getString("message").contains("Invalid option"));
        Assert.assertTrue(response.jsonPath().getString("message").contains("skuPerformance"));
    }
}
