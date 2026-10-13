package ai.metayb.api.auth_session.auth;

import ai.metayb.config.ConfigManager;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.Test;
import testUtils.BaseApiTest;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;

/**
 * POST /web/auth/login - credential-combination and empty-value validation cases that
 * LoginApiTest does not cover (it tests wrong password, unknown user, and each of
 * email/password missing on its own).
 *
 * Login is rate limited to 10 requests per 60 seconds (verified live: ratelimit-limit 10,
 * ratelimit-policy 10;w=60, HTTP 429 "Too many login requests", retry-after header). The
 * suite's own login plus LoginApiTest already use most of that budget, so postLogin() waits
 * out a 429 using the server's retry-after value and repeats the call once. It retries only
 * on 429, never on an assertion failure, so it cannot hide a real defect.
 */
@Epic("BrandRunners Web API")
@Feature("Authentication")
public class LoginValidationApiTest extends BaseApiTest {

    @Test(groups = {"api", "regression", "negative"}, description = "Login with an invalid username AND an invalid password is rejected")
    @Story("User Login")
    @Description("Verified live: HTTP 400, message 'Login Failed' - the same generic message as a wrong password " +
            "alone, so the API does not reveal which of the two was wrong. No accessToken cookie is issued.")
    public void loginWithInvalidUsernameAndInvalidPasswordIsRejected() throws Exception {
        Response response = postLogin(body("nonexistent-user-xyz@metayb.ai", "WrongPassword@999"));

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(response.jsonPath().getString("message"), "Login Failed");
        Assert.assertNull(response.getCookie("accessToken"), "No accessToken cookie should be issued on a failed login");
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Login with both email and password fields missing reports both as required")
    @Story("User Login")
    @Description("Verified live for an empty JSON object: HTTP 400, message 'Validation Error', errors.email " +
            "contains 'Email is required' and errors.password contains 'Password is required' - both field errors " +
            "are reported in the same response.")
    public void loginWithBothFieldsMissingReportsBothRequired() throws Exception {
        Response response = postLogin(new LinkedHashMap<>());

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(response.jsonPath().getString("message"), "Validation Error");
        Assert.assertTrue(response.jsonPath().getList("errors.email", String.class).contains("Email is required"));
        Assert.assertTrue(response.jsonPath().getList("errors.password", String.class).contains("Password is required"));
        Assert.assertNull(response.getCookie("accessToken"));
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Login with empty-string credentials is rejected")
    @Story("User Login")
    @Description("Verified live for email='' and password='': HTTP 400, message 'Validation Error', " +
            "errors.email contains 'Email must be a email'. An empty email is reported as a format error, not as " +
            "'required' (that message is only used when the field is absent). No password error is returned for an " +
            "empty string - the password is only checked against the account afterwards (see the next test).")
    public void loginWithEmptyStringCredentialsIsRejected() throws Exception {
        Response response = postLogin(body("", ""));

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(response.jsonPath().getString("message"), "Validation Error");
        Assert.assertTrue(response.jsonPath().getList("errors.email", String.class).contains("Email must be a email"));
        Assert.assertNull(response.getCookie("accessToken"));
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Login with a valid email and an empty password is rejected as a failed login")
    @Story("User Login")
    @Description("Verified live: HTTP 400, message 'Login Failed' (not a validation error) - an empty password " +
            "string passes request validation and is rejected by authentication instead. No accessToken cookie.")
    public void loginWithValidEmailAndEmptyPasswordIsRejected() throws Exception {
        Response response = postLogin(body(ConfigManager.getApiUsername(), ""));

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(response.jsonPath().getString("message"), "Login Failed");
        Assert.assertNull(response.getCookie("accessToken"));
    }

    @Test(groups = {"api", "regression", "negative"}, description = "Login with null credentials is rejected with type validation errors")
    @Story("User Login")
    @Description("Verified live for email=null and password=null: HTTP 400, message 'Validation Error', " +
            "errors.email contains 'Email must be a email' and errors.password contains 'Password must be a string'.")
    public void loginWithNullCredentialsIsRejected() throws Exception {
        Map<String, Object> nulls = new LinkedHashMap<>();
        nulls.put("email", null);
        nulls.put("password", null);
        Response response = postLogin(nulls);

        Assert.assertEquals(response.statusCode(), 400);
        Assert.assertEquals(response.jsonPath().getString("message"), "Validation Error");
        Assert.assertTrue(response.jsonPath().getList("errors.email", String.class).contains("Email must be a email"));
        Assert.assertTrue(response.jsonPath().getList("errors.password", String.class).contains("Password must be a string"));
    }

    private static Map<String, Object> body(String email, String password) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("email", email);
        body.put("password", password);
        return body;
    }

    private static Response postLogin(Map<String, Object> body) throws Exception {
        String json = new ObjectMapper().writeValueAsString(body);
        Response response = send(json);
        if (response.statusCode() == 429) {
            String retryAfter = response.getHeader("retry-after");
            long waitSeconds = retryAfter != null && retryAfter.matches("\\d+") ? Long.parseLong(retryAfter) : 60;
            Allure.step("Login rate limit (429) - waiting " + (waitSeconds + 1) + "s for the window to reset");
            Thread.sleep((waitSeconds + 1) * 1000);
            response = send(json);
        }
        return response;
    }

    private static Response send(String json) {
        return given().spec(noAuthRequestSpecification)
                .contentType("application/json")
                .body(json)
                .post("/web/auth/login");
    }
}
