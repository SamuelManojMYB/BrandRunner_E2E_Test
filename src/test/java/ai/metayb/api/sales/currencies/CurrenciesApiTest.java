package ai.metayb.api.sales.currencies;

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
 * "04. Sales / Currencies" - all 4 Postman requests, all GET, all working in this tenant.
 *
 * Exploring beyond the 4 Postman requests: an invalid currency code (e.g. "XXX") on
 * "Get Rates by From / To", "Get Rates", or "Convert" hits the same CloudFront/S3 SPA
 * fallback documented elsewhere in this suite, rather than a clean 4xx - not automated
 * as a separate scenario since it isn't one of the 4 Postman requests to begin with, and
 * every Postman request itself is fully automated below with real currency codes.
 */
@Epic("BrandRunners Web API")
@Feature("Sales - Currencies")
public class CurrenciesApiTest extends BaseApiTest {

    @Test(groups = {"api", "smoke", "positive"}, description = "List Currencies returns the real supported currency list")
    @Story("List Currencies")
    @Description("Verified live: HTTP 200, success=true, data.baseCurrency.code='INR', data.currencies is a non-empty array.")
    public void listCurrenciesReturnsRealCurrencyList() {
        Response response = given().spec(requestSpecification).get("/web/currencies");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertEquals(response.jsonPath().getString("data.baseCurrency.code"), "INR");
        Assert.assertFalse(response.jsonPath().getList("data.currencies").isEmpty(), "data.currencies should be non-empty");
    }

    @Test(groups = {"api", "regression", "negative"}, description = "List Currencies without authentication fails")
    @Story("List Currencies")
    @Description("Verified live: HTTP 401, message 'Authentication token missing'.")
    public void listCurrenciesWithoutAuthFails() {
        Response response = given().spec(noAuthRequestSpecification).get("/web/currencies");

        Assert.assertEquals(response.statusCode(), 401);
        Assert.assertEquals(response.jsonPath().getString("message"), "Authentication token missing");
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Rates by From/To returns a real exchange rate")
    @Story("Get Rates by From / To")
    @Description("Verified live for USD -> NGN: HTTP 200, success=true, data.baseCurrency='USD', " +
            "data.targetCurrency='NGN', data.rate is a non-blank real rate value.")
    public void getRatesByFromToReturnsRealRate() {
        Response response = given().spec(requestSpecification).get("/web/currencies/rates/USD/NGN");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertEquals(response.jsonPath().getString("data.baseCurrency"), "USD");
        Assert.assertEquals(response.jsonPath().getString("data.targetCurrency"), "NGN");
        Assert.assertNotNull(response.jsonPath().getString("data.rate"));
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Get Rates returns rates keyed by target currency for a given base")
    @Story("Get Rates")
    @Description("Verified live for base=NGN: HTTP 200, success=true, data.baseCurrency='NGN', " +
            "data.rates is a non-empty map including 'AED' and 'AMD' keys.")
    public void getRatesReturnsRatesMap() {
        Response response = given().spec(requestSpecification).queryParam("base", "NGN").get("/web/currencies/rates");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertEquals(response.jsonPath().getString("data.baseCurrency"), "NGN");
        Assert.assertNotNull(response.jsonPath().getString("data.rates.AED"));
    }

    @Test(groups = {"api", "smoke", "positive"}, description = "Convert converts a real amount between two real currencies")
    @Story("Convert")
    @Description("Verified live for from=NGN, to=USD, amount=1000: HTTP 200, success=true, data.from='NGN', " +
            "data.to='USD', data.amount='1000.00', data.convertedAmount is present and non-blank.")
    public void convertReturnsConvertedAmount() {
        Response response = given().spec(requestSpecification)
                .queryParam("from", "NGN").queryParam("to", "USD").queryParam("amount", "1000")
                .get("/web/currencies/convert");

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertTrue(response.jsonPath().getBoolean("success"));
        Assert.assertEquals(response.jsonPath().getString("data.from"), "NGN");
        Assert.assertEquals(response.jsonPath().getString("data.to"), "USD");
        Assert.assertNotNull(response.jsonPath().getString("data.convertedAmount"));
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Convert without an amount is rejected")
    @Story("Convert")
    @Description("Verified live: HTTP 400, message 'Amount must be a valid decimal'.")
    public void convertWithoutAmountFails() {
        Response response = given().spec(requestSpecification)
                .queryParam("from", "NGN").queryParam("to", "USD")
                .get("/web/currencies/convert");

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(response.jsonPath().getString("message"), "Amount must be a valid decimal");
    }
}
