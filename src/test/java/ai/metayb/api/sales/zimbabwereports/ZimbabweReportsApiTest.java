package ai.metayb.api.sales.zimbabwereports;

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
 * "04. Sales / Zimbabwe Reports" - all 19 Postman requests, all GET, all working in
 * this tenant. id=628 ("beta shoes") and id=63 ("India") below are real, pre-existing
 * records confirmed live, not fabricated.
 */
@Epic("BrandRunners Web API")
@Feature("Sales - Zimbabwe Reports")
public class ZimbabweReportsApiTest extends BaseApiTest {

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Filters Places returns the real location filter tree")
    @Story("Get Filters Places")
    @Description("Verified live: HTTP 200, success=true, data is a non-empty array including id=63 ('India').")
    public void getFiltersPlacesReturnsLocationTree() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/filters/places");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertFalse(response.jsonPath().getList("data").isEmpty());
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Get Filters Places without authentication fails")
    @Story("Get Filters Places")
    @Description("Verified live: HTTP 401, message 'Authentication token missing'.")
    public void getFiltersPlacesWithoutAuthFails() {
        Response response = given().spec(noAuthRequestSpecification).get("/web/zimbabwe-reports/filters/places");

        Assert.assertEquals(response.statusCode(), 401);
        Assert.assertEquals(response.jsonPath().getString("message"), "Authentication token missing");
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Filters Beats returns the real beat filter list")
    @Story("Get Filters Beats")
    @Description("Verified live: HTTP 200, success=true, data is a non-empty array of {id,name,level,code}.")
    public void getFiltersBeatsReturnsBeatList() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/filters/beats");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertFalse(response.jsonPath().getList("data").isEmpty());
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Filters Location Hierarchies returns the real hierarchy level list")
    @Story("Get Filters Location Hierarchies")
    @Description("Verified live: HTTP 200, success=true, data is a non-empty array of {key,label,level,code,hierarchyId}.")
    public void getFiltersLocationHierarchiesReturnsHierarchyLevels() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/filters/location-hierarchies");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertFalse(response.jsonPath().getList("data").isEmpty());
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Filters Locations with a valid level returns real location data")
    @Story("Get Filters Locations")
    @Description("Verified live: the real required query param is 'level' (a number), not 'parentId'/'hierarchyId' " +
            "as tried first - confirmed by trial. HTTP 200 for level=1, success=true, data includes id=63 ('India').")
    public void getFiltersLocationsWithValidLevelReturnsLocations() {
        Response response = given().spec(requestSpecification)
                .queryParam("level", 1).get("/web/zimbabwe-reports/filters/locations");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertFalse(response.jsonPath().getList("data").isEmpty());
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Get Filters Locations without level is rejected")
    @Story("Get Filters Locations")
    @Description("Verified live: HTTP 400, message 'Invalid input: expected number, received NaN' - matches the " +
            "Postman collection's own saved example, which sends this request with no query params.")
    public void getFiltersLocationsWithoutLevelFails() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/filters/locations");

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(response.jsonPath().getString("message"), "Invalid input: expected number, received NaN");
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Filters Sales Reps returns the sales rep filter structure")
    @Story("Get Filters Sales Reps")
    @Description("Verified live: HTTP 200, success=true, data is an array (empty in this tenant).")
    public void getFiltersSalesRepsReturnsStructure() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/filters/sales-reps");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data"));
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Outlets returns the outlet report structure with real location headers")
    @Story("Get Outlets")
    @Description("Verified live: HTTP 200, success=true, data.locationHeaders is a non-empty array of real " +
            "location-hierarchy column definitions (Country/State/District/City), data.pagination present.")
    public void getOutletsReturnsReportStructure() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/outlets");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertFalse(response.jsonPath().getList("data.locationHeaders").isEmpty());
        Assert.assertNotNull(response.jsonPath().getInt("data.pagination.total"));
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Export Outlets returns a base64-encoded export file")
    @Story("Export Outlets")
    @Description("Verified live: HTTP 200, success=true, data.fileBase64 is present and non-blank.")
    public void exportOutletsReturnsFile() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/outlets/export");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertFalse(response.jsonPath().getString("data.fileBase64").isBlank());
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Customers returns the customer report structure")
    @Story("Get Customers")
    @Description("Verified live: HTTP 200, success=true, data.data is an array with pagination metadata.")
    public void getCustomersReturnsReportStructure() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/customers");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data.data"));
        Assert.assertNotNull(response.jsonPath().getInt("data.pagination.total"));
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Export Customers returns a base64-encoded export file")
    @Story("Export Customers")
    @Description("Verified live: HTTP 200, success=true, data.fileBase64 is present and non-blank.")
    public void exportCustomersReturnsFile() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/customers/export");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertFalse(response.jsonPath().getString("data.fileBase64").isBlank());
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Restocking returns the restocking report structure")
    @Story("Get Restocking")
    @Description("Verified live: HTTP 200, success=true, data.data is an array with pagination metadata.")
    public void getRestockingReturnsReportStructure() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/restocking");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data.data"));
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Export Restocking returns a base64-encoded export file")
    @Story("Export Restocking")
    @Description("Verified live: HTTP 200, success=true, data.fileBase64 is present and non-blank.")
    public void exportRestockingReturnsFile() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/restocking/export");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertFalse(response.jsonPath().getString("data.fileBase64").isBlank());
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Products returns real, pre-existing product report rows")
    @Story("Get Products")
    @Description("Verified live: HTTP 200, success=true, data.data is a non-empty array including real product " +
            "id=628 ('beta shoes'), data.pagination.total=17.")
    public void getProductsReturnsRealProductRows() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/products");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertFalse(response.jsonPath().getList("data.data").isEmpty());
        Assert.assertEquals(response.jsonPath().getString("data.data[0].productName"), "beta shoes");
        Assert.assertTrue(response.jsonPath().getInt("data.pagination.total") > 0);
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Export Products returns a base64-encoded export file")
    @Story("Export Products")
    @Description("Verified live: HTTP 200, success=true, data.fileBase64 is present and non-blank.")
    public void exportProductsReturnsFile() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/products/export");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertFalse(response.jsonPath().getString("data.fileBase64").isBlank());
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Sales Reps returns the sales rep report structure")
    @Story("Get Sales Reps")
    @Description("Verified live: HTTP 200, success=true, data.data is an array with pagination metadata.")
    public void getSalesRepsReturnsReportStructure() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/sales-reps");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data.data"));
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Export Sales Reps returns a base64-encoded export file")
    @Story("Export Sales Reps")
    @Description("Verified live: HTTP 200, success=true, data.fileBase64 is present and non-blank.")
    public void exportSalesRepsReturnsFile() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/sales-reps/export");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertFalse(response.jsonPath().getString("data.fileBase64").isBlank());
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Sales returns the sales report structure")
    @Story("Get Sales")
    @Description("Verified live: HTTP 200, success=true, data.data is an array with pagination metadata.")
    public void getSalesReturnsReportStructure() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/sales");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data.data"));
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Export Sales returns a base64-encoded export file")
    @Story("Export Sales")
    @Description("Verified live: HTTP 200, success=true, data.fileBase64 is present and non-blank.")
    public void exportSalesReturnsFile() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/sales/export");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertFalse(response.jsonPath().getString("data.fileBase64").isBlank());
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Sales Details returns the sales details report structure")
    @Story("Get Sales Details")
    @Description("Verified live: HTTP 200, success=true, data.data is an array with pagination metadata.")
    public void getSalesDetailsReturnsReportStructure() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/sales-details");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data.data"));
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Export Sales Details returns a base64-encoded export file")
    @Story("Export Sales Details")
    @Description("Verified live: HTTP 200, success=true, data.fileBase64 is present and non-blank.")
    public void exportSalesDetailsReturnsFile() {
        Response response = given().spec(requestSpecification).get("/web/zimbabwe-reports/sales-details/export");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertFalse(response.jsonPath().getString("data.fileBase64").isBlank());
    }
}
