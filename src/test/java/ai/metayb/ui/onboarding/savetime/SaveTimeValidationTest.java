package ai.metayb.ui.onboarding.savetime;

import io.qameta.allure.*;
import org.testng.annotations.Test;
import testUtils.OnboardingAuthenticatedTest;

/**
 * Scenarios that require clicking Create in the Create Business Unit dialog - save-time validation per module,
 * persistence after refresh or re-login, duplicate names, and the full onboarding workflow. Every test here is
 * disabled on purpose and is listed so the gap stays visible and traceable instead of silently missing.
 *
 * WHY NOT AUTOMATED (verified live on 2026-10-13, nothing was saved):
 *  - Create is enabled in every state - initially, with only a name, and with ERP or CDP set to Active and every
 *    required field empty - so the button's state cannot be used as a safe guard, and the only way to see save-time
 *    behavior is to click it.
 *  - Clicking Create may create a real Business Unit on the shared QA tenant, and no way to remove one afterwards
 *    has been verified. The existing suite deliberately never clicks Create for the same reason.
 *  - What the portal does on submit with incomplete or invalid configuration (blocked client-side, rejected by the
 *    server, or saved anyway) has not been observed, so any assertion would be a guess.
 * To enable these, provide a disposable tenant (or confirmed cleanup) and the expected save-time rules.
 *
 * AREAS WITH NOTHING TO ADD, and the existing tests that show why:
 *  - Tenant Crons (H): the dialog only lists crons with a Remove button and has no schedule input, expression field
 *    or mandatory field (CRN-001..CRN-007, CRN-007 asserts no search/filter/pagination/tabs), so schedule formats and
 *    invalid expressions do not exist to be tested.
 *  - Geofence (I): the section is a status plus a whole-number Radius with range and length validation (GEO-004..
 *    GEO-011); there is no map or location-loading UI.
 *  - OTP Verification (J): the section is four independent on/off switches (OTP-002..OTP-006); there is no code
 *    entry, resend, cooldown or failure-count behavior in this portal.
 *  - Currency (L): a closed dropdown of INR and USD (CUR-004); unsupported values are covered by the "No options"
 *    search result (CUR-007), empty by CUR-002/008/009. There are no amounts, so no decimal precision or rounding,
 *    and no list entries, so no duplicates.
 *  - Dashboard (G): refresh keeps the session and unauthenticated access redirects (DashboardTest). Loading and
 *    error states need simulated network failures, and an empty state needs a tenant with no Business Units; neither
 *    is available against the shared QA tenant.
 *
 * OBSERVED, NOT ASSERTED (the intended rule is not defined, so no test claims either way) - raised as questions:
 *  - No input in the ERP, CDP, Payment or Activity Communication sections has a maximum length: 300 characters were
 *    accepted in every text field, including secrets.
 *  - A whitespace-only value is accepted in a required field (ERP SQL Endpoint, CDP Company Name) while Active, with
 *    no required error.
 */
@Epic("Onboarding Portal")
@Feature("Create Business Unit - Save-time behavior")
public class SaveTimeValidationTest extends OnboardingAuthenticatedTest {

    private static final String SEE_CLASS = "Needs Create to be clicked; see class Javadoc. Not executed.";

    @Test(enabled = false, groups = {"regression", "savetime"}, description = "NOT AUTOMATED (safety): Create with an empty Business Unit name is rejected")
    @Story("Business Unit Details")
    @Description(SEE_CLASS)
    public void createWithEmptyNameIsRejected() {
    }

    @Test(enabled = false, groups = {"regression", "savetime"}, description = "NOT AUTOMATED (safety): Create with a duplicate Business Unit name is rejected")
    @Story("Business Unit Details")
    @Description(SEE_CLASS + " A duplicate can only be produced by first creating a Business Unit.")
    public void createWithDuplicateNameIsRejected() {
    }

    @Test(enabled = false, groups = {"regression", "savetime"}, description = "NOT AUTOMATED (safety): saved Business Unit persists after refresh and re-login")
    @Story("Business Unit Details")
    @Description(SEE_CLASS + " Requires a saved Business Unit to reload.")
    public void savedBusinessUnitPersistsAfterRefreshAndReLogin() {
    }

    @Test(enabled = false, groups = {"regression", "savetime"}, description = "NOT AUTOMATED (safety): Create with Active ERP and empty required fields is rejected")
    @Story("ERP Integration")
    @Description(SEE_CLASS)
    public void createWithIncompleteActiveErpIsRejected() {
    }

    @Test(enabled = false, groups = {"regression", "savetime"}, description = "NOT AUTOMATED (safety): Create with Active CDP and empty required fields is rejected")
    @Story("CDP Integration")
    @Description(SEE_CLASS)
    public void createWithIncompleteActiveCdpIsRejected() {
    }

    @Test(enabled = false, groups = {"regression", "savetime"}, description = "NOT AUTOMATED (safety): Create with Active Payment and empty required fields is rejected")
    @Story("Payment Integration")
    @Description(SEE_CLASS)
    public void createWithIncompleteActivePaymentIsRejected() {
    }

    @Test(enabled = false, groups = {"regression", "savetime"}, description = "NOT AUTOMATED (safety): Create with Active Activity Communication and no API key is rejected")
    @Story("Activity Communication")
    @Description(SEE_CLASS)
    public void createWithIncompleteActiveActivityCommunicationIsRejected() {
    }

    @Test(enabled = false, groups = {"regression", "savetime"}, description = "NOT AUTOMATED (safety): Create with no Currency selected is handled")
    @Story("Currency")
    @Description(SEE_CLASS + " Whether Currency is mandatory at save time is not known.")
    public void createWithNoCurrencyIsHandled() {
    }

    @Test(enabled = false, groups = {"regression", "savetime"}, description = "NOT AUTOMATED (safety): Create with an invalid Geofence radius is rejected")
    @Story("Geofence")
    @Description(SEE_CLASS)
    public void createWithInvalidGeofenceRadiusIsRejected() {
    }

    @Test(enabled = false, groups = {"regression", "savetime"}, description = "NOT AUTOMATED (safety): complete onboarding workflow - login, create and configure a Business Unit, save, verify persisted data")
    @Story("Critical end-to-end workflow")
    @Description(SEE_CLASS + " The existing CriticalE2eReadOnlyTest covers the read-only half against the baseline " +
            "Business Unit 'QA Test BU 01'.")
    public void completeOnboardingWorkflowPersistsConfiguration() {
    }
}
