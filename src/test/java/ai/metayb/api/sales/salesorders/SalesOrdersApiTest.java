package ai.metayb.api.sales.salesorders;

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
 * "04. Sales / Sales Orders" - read requests that return real JSON in this QA tenant.
 * Every "by id" or write route in this Postman subfolder returns the CloudFront/S3 SPA
 * fallback (HTML, not JSON) instead - see SalesOrdersWriteOperationsApiTest's class
 * Javadoc for the live evidence; same systemic root cause already documented across
 * every other automated folder (e.g. LocationWriteOperationsApiTest).
 *
 * This QA tenant has zero real sales/customer/allocation records - every list endpoint
 * below legitimately returns an empty result, which is itself a valid, asserted scenario
 * ("empty valid result"), not a gap. id=1 in "Get Products By Location" is a real,
 * existing location id (returns 14 real priced products) - not fabricated.
 */
@Epic("BrandRunners Web API")
@Feature("Sales - Sales Orders")
public class SalesOrdersApiTest extends BaseApiTest {

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Customers returns the customer list structure")
    @Story("Get Customers")
    @Description("Verified live: HTTP 200, success=true, data is an array (empty in this tenant - no sales customers exist yet).")
    public void getCustomersReturnsCustomerList() {
        Response response = given().spec(requestSpecification).get("/web/sales/customers");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data"), "data should be a list (possibly empty)");
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Get Customers without authentication fails")
    @Story("Get Customers")
    @Description("Verified live: HTTP 401, message 'Authentication token missing'.")
    public void getCustomersWithoutAuthFails() {
        Response response = given().spec(noAuthRequestSpecification).get("/web/sales/customers");

        Assert.assertEquals(response.statusCode(), 401);
        Assert.assertEquals(response.jsonPath().getString("message"), "Authentication token missing");
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Customer Details returns the customer details structure")
    @Story("Get Customer Details")
    @Description("Verified live: HTTP 200, success=true, data is an array (empty in this tenant).")
    public void getCustomerDetailsReturnsStructure() {
        Response response = given().spec(requestSpecification).get("/web/sales/customer-details");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data"));
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Allocations By Distributors with a valid allocationDate returns the allocations structure")
    @Story("Get Allocations By Distributors")
    @Description("Verified live: the real required query param is 'allocationDate' (YYYY-MM-DD), not 'date' as a " +
            "naive reading of the error message might suggest - confirmed by trying both. HTTP 200, success=true, " +
            "data is an array (empty in this tenant).")
    public void getAllocationsByDistributorsWithValidDateReturnsStructure() {
        Response response = given().spec(requestSpecification)
                .queryParam("allocationDate", "2026-09-28")
                .get("/web/sales/allocations-by-distributors");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data"));
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Get Allocations By Distributors without allocationDate is rejected")
    @Story("Get Allocations By Distributors")
    @Description("Verified live: HTTP 400, message 'Allocation date is required (YYYY-MM-DD)'.")
    public void getAllocationsByDistributorsWithoutDateFails() {
        Response response = given().spec(requestSpecification).get("/web/sales/allocations-by-distributors");

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(response.jsonPath().getString("message"), "Allocation date is required (YYYY-MM-DD)");
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Orders List by Allocation ID returns real pricing/contingency stock data")
    @Story("Get Orders List by Allocation ID")
    @Description("Verified live for allocationId=1 (a real, pre-existing allocation): HTTP 200, success=true, " +
            "data.contingencyStock is a non-empty array with real productId/unitPrice/allocatedQty fields, " +
            "data.orders is an empty array (no live sales orders placed against it yet - a valid state).")
    public void getOrdersListByAllocationIdReturnsData() {
        Response response = given().spec(requestSpecification).get("/web/sales/orders-list/1");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertFalse(response.jsonPath().getList("data.contingencyStock").isEmpty(),
                "data.contingencyStock should be non-empty for allocationId=1");
        Assert.assertNotNull(response.jsonPath().getList("data.orders"), "data.orders should be present (possibly empty)");
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Get Invoices By Level1 Location without required params is rejected")
    @Story("Get Invoices By Level1 Location")
    @Description("Verified live: HTTP 400, message 'Invalid input: expected string, received undefined, Invalid " +
            "input: expected number, received NaN' - a Zod-style validation error. Tried multiple plausible query " +
            "param combinations (locationId/date, level1LocationId/date, locationId/locationName) without finding " +
            "the exact required param names/types - the Postman collection's own saved example for this request " +
            "carries no query params either, so this is the only scenario that can be replayed exactly as Postman " +
            "defines it. Documented as validation-only; a true positive would need the real param contract, which " +
            "isn't available from Postman or trial-and-error.")
    public void getInvoicesByLevel1LocationWithoutParamsFails() {
        Response response = given().spec(requestSpecification).get("/web/sales/invoices-by-level1-location");

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertTrue(response.jsonPath().getString("message").contains("Invalid input"));
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Allocated Orders returns the promoter sales orders structure")
    @Story("Get Allocated Orders")
    @Description("Verified live: HTTP 200, success=true, data.data is an array with pagination metadata (total/page/limit/totalPages).")
    public void getAllocatedOrdersReturnsPaginatedStructure() {
        Response response = given().spec(requestSpecification).get("/web/sales/allocated-orders");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data.data"));
        Assert.assertNotNull(response.jsonPath().getInt("data.pagination.total"));
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Get Promoter Journey Plan for a promoter who hasn't clocked in is rejected with a business-rule error")
    @Story("Get Promoter Journey Plan")
    @Description("Verified live with invoiceDate=today (a valid date format): HTTP 400, message 'Clock-in location " +
            "not found. Start your day with location to generate a route plan.' - a real business precondition " +
            "(the logged-in QA user hasn't clocked in today), not a validation error. No positive scenario is " +
            "automated since no clocked-in-promoter test account/state is available in this environment.")
    public void getPromoterJourneyPlanWithoutClockInFails() {
        Response response = given().spec(requestSpecification)
                .queryParam("invoiceDate", "2026-09-28")
                .get("/web/sales/promoter/journey-plan");

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(response.jsonPath().getString("message"),
                "Clock-in location not found. Start your day with location to generate a route plan.");
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Allocations Activity Logs returns the activity log structure")
    @Story("Get Allocations Activity Logs")
    @Description("Verified live for allocationId=1: HTTP 200, success=true, data.data is an array with pagination metadata.")
    public void getAllocationsActivityLogsReturnsStructure() {
        Response response = given().spec(requestSpecification).get("/web/sales/allocations/1/activity-logs");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data.data"));
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Products By Location returns real priced products for a real location")
    @Story("Get Products By Location")
    @Description("Verified live for locationId=1 (a real, pre-existing location): HTTP 200, success=true, " +
            "data.data is a non-empty array of 14 products, each with productId/productName/uoms, and " +
            "data.pagination.total=14 matches the array length.")
    public void getProductsByLocationReturnsRealProducts() {
        Response response = given().spec(requestSpecification).get("/web/sales/get-products-by-location/1");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        java.util.List<Object> products = response.jsonPath().getList("data.data");
        Assert.assertFalse(products.isEmpty(), "data.data should be non-empty for locationId=1");
        Assert.assertEquals((int) response.jsonPath().getInt("data.pagination.total"), products.size(),
                "pagination.total should match the returned product count");
        Assert.assertNotNull(response.jsonPath().getString("data.data[0].productName"));
    }

    @Test(groups = {"api", "regression", "negative"}, description = "CONFIRMED BACKEND DEFECT: Get Products By Location does not filter by locationId at all")
    @Story("Get Products By Location")
    @Description("This is a documented backend defect, not expected/correct behavior - the assertions below pin " +
            "down the real, currently-observed response so this test fails loudly the moment the backend is fixed " +
            "and someone needs to replace it with a real filtering test, rather than silently continuing to " +
            "'pass' a defect forever. Evidence the contract intends filtering: the request is named 'Get Products " +
            "By Location', its own path parameter is literally named ':locationId' (per the Postman collection's " +
            "raw request definition - no test script or saved example exists to confirm expected output, but the " +
            "naming is explicit), and the response schema itself carries location-specific pricing fields " +
            "(data.data[].locationStandardPrice / locationStandardPriceBase) - currently always null, further " +
            "suggesting location-based pricing was designed in but isn't wired up. Live evidence the filter " +
            "doesn't work: called with SIX different locationId values - 1, 2, 3, 5, 10 (a mix of small ids and " +
            "real city ids from /web/zimbabwe-reports/filters/locations?level=4, e.g. 66/68/74) and 999999 (a " +
            "non-existent id) - every single call returns the IDENTICAL 14-product catalog, same first product " +
            "('beta shoes'), same pagination.total=14. The locationId path parameter has no observable effect on " +
            "the response at all, for either valid or invalid values.")
    public void getProductsByLocationDoesNotFilterByLocationIdConfirmedDefect() {
        Response withRealLocation = given().spec(requestSpecification).get("/web/sales/get-products-by-location/1");
        Response withNonExistentLocation = given().spec(requestSpecification).get("/web/sales/get-products-by-location/999999");

        Assert.assertEquals(withNonExistentLocation.statusCode(), 200);
        Assert.assertTrue(withNonExistentLocation.jsonPath().getBoolean("success"));
        Assert.assertEquals(
                withNonExistentLocation.jsonPath().getInt("data.pagination.total"),
                withRealLocation.jsonPath().getInt("data.pagination.total"),
                "BACKEND DEFECT: a non-existent locationId should not return the same product count as a real one");
        Assert.assertEquals(
                withNonExistentLocation.jsonPath().getString("data.data[0].productName"),
                withRealLocation.jsonPath().getString("data.data[0].productName"),
                "BACKEND DEFECT: a non-existent locationId should not return the same products as a real one");
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Notifications by User ID returns the notifications structure")
    @Story("Get Notifications by User ID")
    @Description("Verified live for userId=1 (the logged-in QA user): HTTP 200, success=true, data.notifications is an array.")
    public void getNotificationsByUserIdReturnsStructure() {
        Response response = given().spec(requestSpecification).get("/web/sales/notifications/1");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data.notifications"));
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Contingency Pickup Requests List returns the pickup requests structure")
    @Story("Get Contingency Pickup Requests List")
    @Description("Verified live: HTTP 200, success=true, data is an array (empty in this tenant).")
    public void getContingencyPickupRequestsListReturnsStructure() {
        Response response = given().spec(requestSpecification).get("/web/sales/contingency-pickup-requests-list");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data"));
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Failed Reasons returns the real, pre-configured failure reason taxonomy")
    @Story("Get Failed Reasons")
    @Description("Verified live: HTTP 200, success=true, data is a non-empty array of {type, categories:[{id,key,name," +
            "sortOrder,reasons:[...]}]}. This is stable, tenant-configured reference data (not transient sales data), " +
            "so asserting its real, specific shape is safe and repeatable.")
    public void getFailedReasonsReturnsRealTaxonomy() {
        Response response = given().spec(requestSpecification).get("/web/sales/failed-reasons");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        java.util.List<Object> types = response.jsonPath().getList("data");
        Assert.assertFalse(types.isEmpty(), "data should be non-empty");
        Assert.assertEquals(response.jsonPath().getString("data[0].type"), "FAILED_TO_DELIVER");
        Assert.assertFalse(response.jsonPath().getList("data[0].categories").isEmpty(),
                "data[0].categories should be non-empty");
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get All returns the sales orders list structure")
    @Story("Get All")
    @Description("Verified live: HTTP 200, success=true, data.data is an array with pagination metadata.")
    public void getAllReturnsPaginatedStructure() {
        Response response = given().spec(requestSpecification).get("/web/sales/all");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertNotNull(response.jsonPath().getList("data.data"));
        Assert.assertNotNull(response.jsonPath().getInt("data.pagination.total"));
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Get All with an invalid tenant header is rejected")
    @Story("Get All")
    @Description("Verified live: HTTP 400, message 'Database connection failed, Please check the tenant ID' - same " +
            "tenant-validation behavior already established for other folders (see LoginApiTest).")
    public void getAllWithInvalidTenantFails() {
        // Built manually since requestSpecification hardcodes the real tenant header -
        // reuse the shared auth token/business_unit, only the tenant header differs.
        Response response = given()
                .baseUri(baseURI)
                .header("X-Amz-Tenant-Id", "nonexistent-tenant-xyz")
                .header("Authorization", "Bearer " + authToken)
                .header("business_unit", businessUnitId)
                .get("/web/sales/all");

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(response.jsonPath().getString("message"), "Database connection failed, Please check the tenant ID");
    }
}
