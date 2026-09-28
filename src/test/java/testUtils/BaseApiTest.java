package testUtils;

import ai.metayb.api.utils.SanitizedApiLoggingFilter;
import ai.metayb.config.ConfigManager;
import ai.metayb.utils.PerformanceRecorder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.testng.AllureTestNg;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static io.restassured.RestAssured.given;

@Listeners({AllureTestNg.class})
public class BaseApiTest {

    private static final Logger logger = LogManager.getLogger(BaseApiTest.class);

    protected static final PerformanceRecorder perfRecorder = new PerformanceRecorder();
    protected static String baseURI;
    protected static String authToken;
    // businessInfo is an array in the real response (BrandRunners supports multiple business
    // units per user) - this is the first/default one, needed as the "business_unit" header
    // every session/profile endpoint in "00. Auth & Session" requires.
    protected static String businessUnitId;
    // Captured for RefreshTokenApiTest's positive scenario - confirmed safe to reuse across
    // the whole suite: this API does not rotate/invalidate tokens on refresh (verified live).
    protected static String refreshToken;
    protected static RequestSpecification requestSpecification;
    protected static RequestSpecification noAuthRequestSpecification;

    @BeforeSuite
    public void globalSetup() throws Exception {
        baseURI = ConfigManager.getApiBaseUrl();
        logger.info("Base URI initialized for environment: {} (tenant: {})", baseURI, ConfigManager.getApiTenant());

        // ConfigManager.getApiUsername()/getApiPassword(): API_USERNAME/API_PASSWORD env
        // vars (set by CodeBuild from SSM Parameter Store) take precedence, falling back
        // to DataReader's apiEmail/apiPassword (credentials.properties) for local runs -
        // see ConfigManager for the full precedence chain.
        Response loginResponse = performLogin(ConfigManager.getApiUsername(), ConfigManager.getApiPassword());

        // Per the BrandRunners Web APIs Postman collection's Login test script, the session
        // token is returned as the "accessToken" cookie, not a field in the JSON body.
        authToken = loginResponse.getCookie("accessToken");
        if (authToken == null || authToken.isEmpty()) {
            throw new RuntimeException("Login failed - no accessToken cookie in response. Full response:\n" + loginResponse.asString());
        }
        refreshToken = loginResponse.getCookie("refreshToken");
        businessUnitId = loginResponse.jsonPath().getString("data.user.businessInfo[0].id");
        logger.info("Auth token acquired successfully.");

        requestSpecification = new RequestSpecBuilder()
                .setBaseUri(baseURI)
                .addHeader("X-Amz-Tenant-Id", ConfigManager.getApiTenant())
                .addHeader("Authorization", "Bearer " + authToken)
                .addHeader("business_unit", businessUnitId)
                .addFilter(new SanitizedApiLoggingFilter())
                .build();

        noAuthRequestSpecification = new RequestSpecBuilder()
                .setBaseUri(baseURI)
                .addHeader("X-Amz-Tenant-Id", ConfigManager.getApiTenant())
                .addFilter(new SanitizedApiLoggingFilter())
                .build();
    }

    @AfterMethod
    public void recordMetrics() {
        perfRecorder.generateReport();
    }

    protected void recordResponseTime(long startTime, String endpoint) {
        long responseTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        perfRecorder.record(endpoint, responseTime);
    }

    /**
     * POST /web/auth/login (BrandRunners Web APIs Postman collection, "00. Auth & Session/Auth/Login").
     * Unauthenticated by design - never attach the Authorization-bearing requestSpecification here.
     */
    protected static Response performLogin(String email, String password) throws Exception {
        return given()
                .filter(new SanitizedApiLoggingFilter())
                .baseUri(baseURI)
                .header("X-Amz-Tenant-Id", ConfigManager.getApiTenant())
                .contentType("application/json")
                .body(loginPayload(email, password))
                .post("/web/auth/login");
    }

    private static String loginPayload(String email, String password) throws Exception {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        return new ObjectMapper().writeValueAsString(payload);
    }
}
