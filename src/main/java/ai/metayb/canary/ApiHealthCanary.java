package ai.metayb.canary;

import ai.metayb.api.client.ApiSession;
import ai.metayb.api.client.CanaryApiSessionClient;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import software.amazon.synthetics.Synthetics;

import java.time.LocalDate;

import static ai.metayb.canary.CanaryAssertions.requireField;
import static ai.metayb.canary.CanaryAssertions.requireNonEmptyList;
import static ai.metayb.canary.CanaryAssertions.requireStatus;
import static io.restassured.RestAssured.given;

/**
 * CloudWatch Synthetics entry point for the BrandRunners API health/smoke
 * journey. Handler: ai.metayb.canary.ApiHealthCanary::canaryCode
 *
 * This is NOT a TestNG test and never invokes one - it calls the same
 * BrandRunners Web API endpoints six of the existing, live-verified TestNG
 * tests already cover (see each step's Javadoc for exactly which test that
 * evidence came from), through the same REST Assured + SanitizedApiLoggingFilter
 * path, but through CanaryApiSessionClient (main-scope) instead of
 * BaseApiTest (test-scope, static suite-level auth state - see that class's
 * own Javadoc for why it isn't reused directly).
 *
 * Steps run sequentially because they have a real dependency: every step
 * after Login needs the access token and business_unit id Login produces.
 */
public class ApiHealthCanary {

    static {
        // Must run before any class in this JVM first calls LogManager.getLogger(...) -
        // ApiHealthCanary is the handler class Synthetics loads and invokes, so this
        // static initializer is guaranteed to run first, before this class's own
        // Logger field below and before any other canary class is touched.
        System.setProperty("log4j2.configurationFile", "classpath:log4j2-canary.xml");
    }

    private static final Logger logger = LogManager.getLogger(ApiHealthCanary.class);

    public void canaryCode(Synthetics synthetics) throws Exception {
        logger.info("Starting Canary");
        // TEMPORARY diagnostic marker - same class, same logger instance as the
        // "Starting Canary" line immediately above, which CloudWatch already proves
        // executes. If this line is ALSO absent from CloudWatch despite that, the
        // problem isn't specific to CanaryConfig/CanaryApiSessionClient's loggers -
        // it's something about output ordering/timing/truncation within this exact
        // invocation. Remove once the root cause is confirmed and fixed.
        logger.info("CANARY_BUILD_MARKER=20260925-01");

        CanaryConfig config = CanaryConfig.load();
        logger.info("Environment: {} (tenant: {})", config.getBaseUri(), config.getTenant());

        ApiSession[] sessionHolder = new ApiSession[1];

        synthetics.executeStep("Login", () -> {
            // TEMPORARY diagnostic marker - same class, same logger instance as
            // "Starting Login step" below, immediately adjacent to it.
            logger.info("CANARY_LOGIN_PATH_MARKER=20260925-01");
            logger.info("Starting Login step");
            sessionHolder[0] = CanaryApiSessionClient.login(
                    config.getBaseUri(), config.getTenant(), config.getUsername(), config.getPassword());
            logger.info("Login successful");
            return null;
        }).get();

        RequestSpecification authSpec = CanaryApiSessionClient.buildAuthenticatedRequestSpec(
                config.getBaseUri(), config.getTenant(), sessionHolder[0]);

        // Step 2 - GET /web/session. Verified live in CurrentSessionApiTest:
        // HTTP 200 with data.id/email/roleName and a non-empty permissions list.
        synthetics.executeStep("Current Session", () -> {
            logger.info("Starting Current Session step");
            Response response = given().spec(authSpec).get("/web/session");
            requireStatus("Current Session", response, 200);
            requireField("Current Session", response, "data.id");
            requireField("Current Session", response, "data.email");
            requireField("Current Session", response, "data.roleName");
            requireNonEmptyList("Current Session", response, "data.permissions");
            logger.info("Current Session successful");
            return null;
        }).get();

        // Step 3 - GET /web/settings/app-settings. Verified live in GetAppSettingsApiTest:
        // HTTP 200 with data.masterData.locationHierarchies and data.storedData.appSettings.
        synthetics.executeStep("Control Settings", () -> {
            logger.info("Starting Control Settings step");
            Response response = given().spec(authSpec).get("/web/settings/app-settings");
            requireStatus("Control Settings", response, 200);
            requireField("Control Settings", response, "data.masterData.locationHierarchies");
            requireField("Control Settings", response, "data.storedData.appSettings");
            logger.info("Control Settings successful");
            return null;
        }).get();

        // Step 4 - GET /web/dashboard/counter. Verified live in HomeDashboardApiTest
        // ("Get Counter"): HTTP 200 with data.totalUsers/data.totalTasks.
        synthetics.executeStep("ANP Health", () -> {
            logger.info("Starting ANP Health step");
            Response response = given().spec(authSpec).get("/web/dashboard/counter");
            requireStatus("ANP Health", response, 200);
            requireField("ANP Health", response, "data.totalUsers");
            requireField("ANP Health", response, "data.totalTasks");
            logger.info("ANP Health successful");
            return null;
        }).get();

        // Step 5 - GET /web/fwm-dashboard/status-summary. Verified live in
        // FwmDashboardApiTest ("Status Summary"): HTTP 200 with data.statusSummary.
        // Date is computed fresh each run, exactly as FwmDashboardApiTest does -
        // never hardcoded.
        synthetics.executeStep("FWM Health", () -> {
            logger.info("Starting FWM Health step");
            String today = LocalDate.now().toString();
            Response response = given().spec(authSpec)
                    .get("/web/fwm-dashboard/status-summary?date=" + today + "&locationId=null&designationId=null");
            requireStatus("FWM Health", response, 200);
            requireField("FWM Health", response, "data.statusSummary");
            logger.info("FWM Health successful");
            return null;
        }).get();

        // Step 6 - GET /web/sales-report/activity-lists. Verified live in
        // ReportsSalesApiTest ("Get Activity Lists"): HTTP 200 with a non-empty
        // data.details array. Stand-in for a true "04. Sales" smoke endpoint, which
        // does not exist yet - that Postman folder has never been automated/verified
        // (see the architecture proposal for this gap).
        synthetics.executeStep("Sales Health", () -> {
            logger.info("Starting Sales Health step");
            Response response = given().spec(authSpec).get("/web/sales-report/activity-lists");
            requireStatus("Sales Health", response, 200);
            requireNonEmptyList("Sales Health", response, "data.details");
            logger.info("Sales Health successful");
            return null;
        }).get();

        logger.info("Canary completed successfully");
    }
}
