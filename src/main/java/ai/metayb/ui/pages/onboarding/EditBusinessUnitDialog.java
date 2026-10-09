package ai.metayb.ui.pages.onboarding;

import ai.metayb.ui.components.onboarding.DashboardPageElements;
import ai.metayb.ui.components.onboarding.EditBusinessUnitElements;
import ai.metayb.ui.components.onboarding.ErpField;
import ai.metayb.ui.components.onboarding.OtpFlow;
import ai.metayb.ui.wrappers.BaseDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * The Edit Business Unit Settings dialog, opened from a Business Unit card's menu (Edit Details). READ-ONLY by
 * construction: it can open the dialog, wait for its settings to load, read what is shown and leave through Cancel.
 * There is no method that types, selects, toggles, uploads or clicks Update, so nothing can be saved through it.
 *
 * The "Unsaved changes" chip is not exposed on purpose: the portal can show it before any edit, so it says nothing about
 * whether a change happened.
 *
 * Values that are not secret (name, currency, status values) are read and reported. Anything that could be a secret is
 * only ever checked for being empty or filled, through a read that is never logged, so no value reaches a log, report
 * or assertion message.
 */
public class EditBusinessUnitDialog extends BaseDriver {

    public static final String BUSINESS_UNIT_DETAILS = "Business Unit Details";
    public static final String CURRENCY = "Currency";
    public static final String ERP = "ERP Integration";
    public static final String PAYMENT = "Payment Integration";
    public static final String CDP = "CDP Integration";
    public static final String OTP = "OTP Verification";
    public static final String GEOFENCE = "Geofence";
    public static final String ACTIVITY = "Activity Communication";
    public static final List<String> SECTIONS =
            List.of(BUSINESS_UNIT_DETAILS, CURRENCY, ERP, PAYMENT, CDP, OTP, GEOFENCE, ACTIVITY);

    private static final int LOAD_TIMEOUT_SECONDS = 30;
    private static final String EDIT_DETAILS_XPATH = "//ul[@role='menu']//li[@role='menuitem'][normalize-space()='Edit Details']";

    // ---------------------------------------------------------------- opening / loading / closing

    /**
     * Opens Edit Details for the card whose name is exactly {@code businessUnitName} (never by position) and waits until
     * the settings have loaded.
     */
    public void open(String businessUnitName) {
        new DashboardPage().openBusinessUnitMenu(businessUnitName);
        By item = By.xpath(EDIT_DETAILS_XPATH);
        waitForElementClickable(item);
        click(locateElement("xpath", EDIT_DETAILS_XPATH));
        waitUntilLoaded();
    }

    /**
     * Waits until the dialog is open, no progress indicator remains, the Update button has replaced its "Saving..."
     * loading caption and the name field has been filled from the saved settings.
     */
    public void waitUntilLoaded() {
        try {
            new WebDriverWait(getDriver(), Duration.ofSeconds(LOAD_TIMEOUT_SECONDS)).until(d ->
                    !d.findElements(EditBusinessUnitElements.dialog()).isEmpty()
                            && d.findElements(EditBusinessUnitElements.progressIndicators()).isEmpty()
                            && !d.findElements(EditBusinessUnitElements.updateButtonReady()).isEmpty()
                            && !isBlank(d.findElement(EditBusinessUnitElements.name()).getDomProperty("value")));
            reportStep("The Edit Business Unit Settings dialog has finished loading", "PASS");
        } catch (Exception e) {
            reportStep("The Edit Business Unit Settings dialog did not finish loading within "
                    + LOAD_TIMEOUT_SECONDS + " seconds", "FAIL");
        }
    }

    public boolean isOpen() {
        return isPresent(EditBusinessUnitElements.dialog());
    }

    /** Leaves the dialog without saving. */
    public void cancel() {
        click(locateElement("xpath", stripLocator(EditBusinessUnitElements.cancelButton())));
        waitForElementInvisible(EditBusinessUnitElements.dialog());
    }

    public boolean isOnDashboardRoute() {
        return "/dashboard".equals(java.net.URI.create(getCurrentUrl()).getPath());
    }

    // ---------------------------------------------------------------- frame

    /** The title without the "Unsaved changes" chip, e.g. "Edit Business Unit Settings". */
    public String getTitle() {
        String text = getText(locateElement("xpath", EditBusinessUnitElements.DIALOG_XPATH + "//h2")).replaceAll("\\s+", " ");
        return text.replace(EditBusinessUnitElements.UNSAVED_CHANGES_CHIP_TEXT, "").trim();
    }

    public boolean isSectionDisplayed(String section) {
        return isPresent(EditBusinessUnitElements.sectionHeading(section));
    }

    // ---------------------------------------------------------------- Business Unit Details

    public String getName() {
        return readValue(EditBusinessUnitElements.name(), "Business Unit name");
    }

    /** "active" / "inactive", the machine value of the Status select. */
    public String getBusinessUnitStatus() {
        return readValue(EditBusinessUnitElements.businessUnitStatusValue(), "Business Unit status");
    }

    // ---------------------------------------------------------------- Currency

    /** The selected currency as displayed, e.g. "INR (₹)". */
    public String getCurrency() {
        return readValue(EditBusinessUnitElements.currencyInput(), "Currency");
    }

    // ---------------------------------------------------------------- integration statuses ("active" / "inactive")

    public String getErpStatus() {
        return readValue(EditBusinessUnitElements.configStatusValue(ERP, "ERP Config Status"), "ERP status");
    }

    public String getPaymentStatus() {
        return readValue(EditBusinessUnitElements.configStatusValue(PAYMENT, "Payment Config Status"), "Payment status");
    }

    public String getCdpStatus() {
        return readValue(EditBusinessUnitElements.configStatusValue(CDP, "CDP Config Status"), "CDP status");
    }

    public String getGeofenceStatus() {
        return readValue(EditBusinessUnitElements.configStatusValue(GEOFENCE, "Geofence Config Status"), "Geofence status");
    }

    public String getActivityStatus() {
        return readValue(EditBusinessUnitElements.namedInput(ACTIVITY, "status"), "Activity Communication status");
    }

    // ---------------------------------------------------------------- emptiness checks (values are never reported)

    public boolean isErpProviderEmpty() {
        return isEmpty(EditBusinessUnitElements.autocompleteInput(ERP, "ERP Provider"), "ERP Provider");
    }

    /** Whether one ERP field is empty. The field's value is never read into a report, whether or not it is a secret. */
    public boolean isErpFieldEmpty(ErpField field) {
        return isEmpty(EditBusinessUnitElements.namedInput(ERP, field.inputName()), "ERP field '" + field.label() + "'");
    }

    public boolean isPaymentProviderEmpty() {
        return isEmpty(EditBusinessUnitElements.autocompleteInput(PAYMENT, "Payment Provider"), "Payment Provider");
    }

    public boolean isPaymentSecretEmpty() {
        return isEmpty(EditBusinessUnitElements.namedInput(PAYMENT, "PAYSTACK_SECRET_KEY"), "Payment secret");
    }

    public boolean isCdpCompanyNameEmpty() {
        return isEmpty(EditBusinessUnitElements.namedInput(CDP, "CDP_COMPANY_NAME"), "CDP Company Name");
    }

    public boolean isCdpCountryEmpty() {
        return isEmpty(EditBusinessUnitElements.autocompleteInput(CDP, "Country"), "CDP Country");
    }

    public boolean isGeofenceRadiusEmpty() {
        return isEmpty(EditBusinessUnitElements.namedInput(GEOFENCE, "radius"), "Geofence radius");
    }

    public boolean isActivityChannelEmpty() {
        return isEmpty(EditBusinessUnitElements.namedInput(ACTIVITY, "channel"), "Activity Communication channel");
    }

    /**
     * Whether the API Key and Sender ID fields exist. They appear only once a channel is chosen, so with no channel they
     * are expected to be absent, which also means no API key is configured. Nothing is read from them.
     */
    public boolean isActivityApiKeyFieldPresent() {
        return !getDriver().findElements(EditBusinessUnitElements.namedInput(ACTIVITY, "api_key")).isEmpty();
    }

    public boolean isActivitySenderIdFieldPresent() {
        return !getDriver().findElements(EditBusinessUnitElements.namedInput(ACTIVITY, "senderId")).isEmpty();
    }

    // ---------------------------------------------------------------- OTP

    public boolean isOtpOn(OtpFlow flow) {
        WebElement toggle = locateElement("xpath", stripLocator(EditBusinessUnitElements.otpSwitch(flow)));
        return isSelected(toggle);
    }

    // ---------------------------------------------------------------- helpers

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String stripLocator(By locator) {
        return locator.toString().replaceFirst("^By\\.xpath: ", "");
    }

    /** Reads the DOM value of a non-secret field and reports it with its description. */
    private String readValue(By locator, String description) {
        try {
            String value = getDriver().findElement(locator).getDomProperty("value");
            String shown = value == null ? "" : value.trim();
            reportStep(description + " is '" + shown + "'", "INFO");
            return shown;
        } catch (Exception e) {
            reportStep("Could not read the " + description + ": " + e.getMessage(), "FAIL");
            return "";
        }
    }

    /** Whether a field is empty. Only "empty" or "filled" is reported; the value is never logged. */
    private boolean isEmpty(By locator, String description) {
        try {
            String value = getDriver().findElement(locator).getDomProperty("value");
            boolean empty = value == null || value.isEmpty();
            reportStep(description + " is " + (empty ? "empty" : "filled"), "INFO");
            return empty;
        } catch (Exception e) {
            reportStep("Could not tell whether the " + description + " is empty: " + e.getMessage(), "FAIL");
            return false;
        }
    }
}
