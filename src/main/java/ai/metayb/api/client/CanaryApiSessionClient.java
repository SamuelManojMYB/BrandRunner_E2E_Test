package ai.metayb.api.client;

import ai.metayb.api.utils.SanitizedApiLoggingFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * The minimum reusable login/session logic the CloudWatch Canary needs -
 * extracted from {@code testUtils.BaseApiTest} rather than reusing that class
 * directly.
 *
 * BaseApiTest can't be reused as-is for two independent reasons: (1) it lives
 * under src/test/java, so a plain `mvn package` never puts it in the runtime
 * jar a Lambda-based Canary loads; (2) its auth state (authToken,
 * businessUnitId) is static and populated once per TestNG suite run via
 * {@code @BeforeSuite} - exactly the "static TestNG authentication state" a
 * Canary must NOT reuse, since each Canary invocation needs its own fresh
 * login. BaseApiTest itself is intentionally left untouched; this class
 * duplicates its ~20 lines of login logic rather than risk touching a base
 * class every existing API test extends.
 */
public final class CanaryApiSessionClient {

    private static final Logger logger = LogManager.getLogger(CanaryApiSessionClient.class);

    // TEMPORARY - the credential diagnostics below are logged through THIS SAME
    // Logger instance (LogManager.getLogger(SanitizedApiLoggingFilter.class)) as
    // the one that already produces the proven-working "--> METHOD URL" line for
    // every request, rather than this class's own `logger` field above. This
    // isolates whether per-class Logger identity matters, now that a same-class/
    // same-logger marker in ApiHealthCanary is also being tested. Remove once the
    // root cause is confirmed and fixed.
    private static final Logger sameLoggerAsRequestFilter = LogManager.getLogger(SanitizedApiLoggingFilter.class);

    private CanaryApiSessionClient() {
    }

    /**
     * POST /web/auth/login (BrandRunners Web APIs Postman collection,
     * "00. Auth & Session / Auth / Login") - same request BaseApiTest.performLogin
     * makes, reproduced here so it's callable from main-scope code.
     */
    public static ApiSession login(String baseUri, String tenant, String email, String password) throws Exception {
        logLoginRequestDiagnostics(baseUri, tenant, email, password);

        Response response = given()
                .filter(new SanitizedApiLoggingFilter())
                .baseUri(baseUri)
                .header("X-Amz-Tenant-Id", tenant)
                .contentType("application/json")
                .body(loginPayload(email, password))
                .post("/web/auth/login");

        String authToken = response.getCookie("accessToken");
        if (authToken == null || authToken.isEmpty()) {
            throw new IllegalStateException(
                    "Login failed - no accessToken cookie in response (status " + response.statusCode() + ")");
        }

        String businessUnitId = response.jsonPath().getString("data.user.businessInfo[0].id");
        if (businessUnitId == null || businessUnitId.isEmpty()) {
            throw new IllegalStateException("Login succeeded but no business unit id was returned");
        }

        return new ApiSession(authToken, businessUnitId);
    }

    /**
     * Same header shape as BaseApiTest.requestSpecification: tenant + bearer
     * token + business_unit, with the existing secret-masking filter attached.
     */
    public static RequestSpecification buildAuthenticatedRequestSpec(String baseUri, String tenant, ApiSession session) {
        return new RequestSpecBuilder()
                .setBaseUri(baseUri)
                .addHeader("X-Amz-Tenant-Id", tenant)
                .addHeader("Authorization", "Bearer " + session.authToken())
                .addHeader("business_unit", session.businessUnitId())
                .addFilter(new SanitizedApiLoggingFilter())
                .build();
    }

    private static String loginPayload(String email, String password) throws Exception {
        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("email", email);
        payload.put("password", password);
        return new ObjectMapper().writeValueAsString(payload);
    }

    /**
     * TEMPORARY diagnostic logging - added to investigate why login succeeds
     * locally but returns HTTP 400 "Login Failed" when the same code runs as
     * a CloudWatch Canary. Remove once the root cause is confirmed and fixed.
     *
     * Logged through Log4j2 (not System.out/System.err): cross-referencing the
     * deployed Canary's actual CloudWatch output against source proved that
     * every line which reaches CloudWatch - ApiHealthCanary's "Starting Canary"/
     * "Environment: ..." logger.info calls, SanitizedApiLoggingFilter's
     * "--> METHOD URL" logger.info call - is Log4j2 Logger-based, while two
     * prior rounds of raw System.out/System.err diagnostics never appeared at
     * all, even though the real login POST that follows immediately after
     * clearly does execute (its real request/response shows up in CloudWatch).
     * The Synthetics Java runtime's CloudWatch log capture is wired to the
     * Log4j2 appender pipeline (see log4j2-canary.xml's ConsoleAppender),
     * which is set up before the runtime does whatever it does to System.out/
     * System.err for its own step/HAR reporting - so raw println calls after
     * that point don't reach the same sink Log4j2 already has a handle on.
     *
     * Reports the ACTUAL JSON field names loginPayload() uses ("email", not
     * "username" - confirmed directly from that method below) with values
     * masked. Never logs the actual username/password/token value - only
     * lengths, whitespace *detection* (not the value that revealed it), and
     * Parameter Store parameter *names* (not their contents). Neither value
     * is ever trimmed anywhere in this class.
     */
    private static void logLoginRequestDiagnostics(String baseUri, String tenant, String email, String password) {
        String usernameParam = System.getenv("CANARY_USERNAME_PARAMETER");
        String passwordParam = System.getenv("CANARY_PASSWORD_PARAMETER");
        String usernameEnv = System.getenv("CANARY_USERNAME");

        // Mirrors CanaryConfig.load()'s exact precedence (CANARY_USERNAME env var beats
        // CANARY_USERNAME_PARAMETER; CANARY_PASSWORD_PARAMETER beats CANARY_PASSWORD env
        // var), so this reports which source was ACTUALLY used, not just which env vars
        // happen to be set.
        boolean usernameFromSsm = (usernameEnv == null || usernameEnv.isBlank())
                && usernameParam != null && !usernameParam.isBlank();
        boolean passwordFromSsm = passwordParam != null && !passwordParam.isBlank();
        String credentialSource = passwordFromSsm ? "SSM" : (usernameFromSsm ? "SSM (username only)" : "non-SSM");

        String[] lines = {
                "CredentialSource=" + credentialSource,
                "UsernameParameter=" + (usernameParam == null || usernameParam.isBlank() ? "(not set)" : usernameParam),
                "UsernameRetrieved=" + (email != null && !email.isBlank()),
                "UsernameLength=" + (email == null ? 0 : email.length()),
                "UsernameWhitespace=" + (email != null && !email.equals(email.trim())),
                "PasswordParameter=" + (passwordParam == null || passwordParam.isBlank() ? "(not set)" : passwordParam),
                "PasswordRetrieved=" + (password != null && !password.isBlank()),
                "PasswordLength=" + (password == null ? 0 : password.length()),
                "PasswordWhitespace=" + (password != null && !password.equals(password.trim())),
                "Final login URL=" + baseUri + "/web/auth/login",
                "Request Content-Type=application/json",
                "Tenant header name=X-Amz-Tenant-Id",
                "Tenant header value=" + tenant,
                // loginPayload() below builds {"email": ..., "password": ...} - these are
                // the real field names, not a placeholder "username"/"password" guess.
                "Login payload={\"email\":\"***\",\"password\":\"***\"}"
        };
        for (String line : lines) {
            sameLoggerAsRequestFilter.info(line);
        }
    }
}
