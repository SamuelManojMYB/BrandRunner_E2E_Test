package ai.metayb.api.auth_session.auth;

import ai.metayb.config.ConfigManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.BaseApiTest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * POST /web/auth/forgot-password - "00. Auth & Session / Auth / Forgot Password".
 *
 * This endpoint enforces a HARD limit of 3 requests per hour (confirmed live via its
 * response headers: x-ratelimit-limit=3, x-ratelimit-remaining, retry-after in
 * seconds) - a window measured in seconds of test pacing cannot work around it.
 * Deliberately kept to exactly 3 automated calls below (1 positive + 2 negative) so a
 * fresh hourly window can run this class cleanly end to end. A 4th scenario - malformed
 * email format ("not-an-email") - was verified live (HTTP 400, "Validation Error",
 * errors.email: ["Email must be a email"]) but intentionally NOT kept as a permanent
 * test, since it would push this class over budget every run; see the final report's
 * Issues section.
 */
@Epic("BrandRunners Web API")
@Feature("Authentication")
public class ForgotPasswordApiTest extends BaseApiTest {

    @Test(groups = {"api", "smoke", "positive"}, description = "Forgot password with a known, registered email succeeds")
    @Story("Forgot Password")
    @Description("Verified live: HTTP 200, message 'A reset link has been sent to your email'.")
    public void forgotPasswordWithKnownEmailSucceeds() throws Exception {
        Response response = forgotPassword(ConfigManager.getApiUsername());

        Assert.assertEquals(response.statusCode(), 200);
        Assert.assertEquals(response.jsonPath().getString("message"), "A reset link has been sent to your email");
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Forgot password with an unregistered email is rejected")
    @Story("Forgot Password")
    @Description("Verified live: HTTP 400, message 'This email is not registered. Please register before using " +
            "forgot password' - unlike Login, this endpoint DOES reveal whether an email is registered.")
    public void forgotPasswordWithUnknownEmailIsRejected() throws Exception {
        Response response = forgotPassword("totally-unknown-user-abcxyz@metayb.ai");

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(response.jsonPath().getString("message"),
                "This email is not registered. Please register before using forgot password");
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Forgot password with the email field missing is rejected")
    @Story("Forgot Password")
    @Description("Verified live: HTTP 400, message 'Validation Error', errors.email contains 'Email is required'.")
    public void forgotPasswordWithMissingEmailFieldIsRejected() throws Exception {
        Response response = given().spec(noAuthRequestSpecification)
                .contentType("application/json")
                .body(new ObjectMapper().writeValueAsString(Collections.emptyMap()))
                .post("/web/auth/forgot-password");

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(response.jsonPath().getString("message"), "Validation Error");
        Assert.assertTrue(response.jsonPath().getList("errors.email", String.class).contains("Email is required"));
    }

    private static Response forgotPassword(String email) throws Exception {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("email", email);
        return given().spec(noAuthRequestSpecification)
                .contentType("application/json")
                .body(new ObjectMapper().writeValueAsString(body))
                .post("/web/auth/forgot-password");
    }
}
