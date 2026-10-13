package ai.metayb.api.auth_session.session;

import ai.metayb.api.utils.SanitizedApiLoggingFilter;
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

import static io.restassured.RestAssured.given;

/**
 * API-level equivalent of "session behavior after refresh": the server-side contract the
 * browser relies on when it silently refreshes tokens. Combines POST /web/auth/refresh with
 * GET /web/session. Uses the suite-level refresh token, so no extra login is spent against
 * the 10-per-minute login rate limit.
 *
 * Verified live: refresh returns a NEW accessToken cookie, the previous access token stays
 * valid, and /web/session returns the same user for both tokens.
 */
@Epic("BrandRunners Web API")
@Feature("Authentication")
public class SessionAfterRefreshApiTest extends BaseApiTest {

    @Test(groups = {"api", "smoke", "positive"}, description = "A refreshed access token resolves to the same user session")
    @Story("Session after token refresh")
    @Description("Verified live: POST /web/auth/refresh returns HTTP 200 and sets a new accessToken cookie that " +
            "differs from the one issued at login; GET /web/session with the refreshed token returns HTTP 200 and " +
            "the same user id and email as the session obtained with the original token.")
    public void refreshedAccessTokenReturnsSameUserSession() throws Exception {
        Response original = sessionWith(authToken);
        Assert.assertEquals(original.statusCode(), 200, "Baseline session should be available");

        Response refresh = given().spec(noAuthRequestSpecification)
                .contentType("application/json")
                .body(new ObjectMapper().writeValueAsString(Collections.singletonMap("refreshToken", refreshToken)))
                .post("/web/auth/refresh");
        Assert.assertEquals(refresh.statusCode(), 200);
        String refreshedToken = refresh.getCookie("accessToken");
        Assert.assertNotNull(refreshedToken, "Refresh should issue a new accessToken cookie");
        Assert.assertNotEquals(refreshedToken, authToken, "Refresh should issue a different access token");

        Response afterRefresh = sessionWith(refreshedToken);
        Assert.assertEquals(afterRefresh.statusCode(), 200, "Refreshed token should open a valid session");
        Assert.assertEquals(afterRefresh.jsonPath().getString("data.id"), original.jsonPath().getString("data.id"));
        Assert.assertEquals(afterRefresh.jsonPath().getString("data.email"), original.jsonPath().getString("data.email"));
    }

    @Test(groups = {"api", "regression", "positive"}, description = "The original access token remains valid after a refresh")
    @Story("Session after token refresh")
    @Description("Verified live: refreshing does not invalidate the access token issued at login - GET /web/session " +
            "with the original token still returns HTTP 200 afterwards. (Documented as observed behavior; the same " +
            "property is what lets the whole suite share one login across tests.)")
    public void originalAccessTokenStillValidAfterRefresh() throws Exception {
        given().spec(noAuthRequestSpecification)
                .contentType("application/json")
                .body(new ObjectMapper().writeValueAsString(Collections.singletonMap("refreshToken", refreshToken)))
                .post("/web/auth/refresh");

        Response session = sessionWith(authToken);
        Assert.assertEquals(session.statusCode(), 200);
        Assert.assertTrue(session.jsonPath().getBoolean("success"));
    }

    private static Response sessionWith(String accessToken) {
        return given()
                .filter(new SanitizedApiLoggingFilter())
                .baseUri(baseURI)
                .header("X-Amz-Tenant-Id", ConfigManager.getApiTenant())
                .header("Authorization", "Bearer " + accessToken)
                .header("business_unit", businessUnitId)
                .get("/web/session");
    }
}
