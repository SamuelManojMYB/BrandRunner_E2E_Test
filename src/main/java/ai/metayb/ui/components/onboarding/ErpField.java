package ai.metayb.ui.components.onboarding;

/**
 * The nine plain text inputs of the ERP Integration section, keyed by the stable {@code name} attribute the portal
 * gives them. The label is the visible caption without the required marker (the portal appends " *" to it while the
 * status is Active), and the required message is exactly what the portal shows when the field is edited to empty
 * while Active. The messages are not derived from the labels on purpose: they differ in the portal itself.
 */
public enum ErpField {

    SQL_ENDPOINT("FABRIC_SQL_ENDPOINT", "SQL Endpoint", "SQL Endpoint is required.", false),
    DATABASE("FABRIC_DATABASE", "Database", "Database name is required.", false),
    MS_TENANT_ID("MS_TENANT_ID", "MS Tenant ID", "MS Tenant ID is required.", false),
    CLIENT_ID("MS_CLIENT_ID", "Client ID", "MS Client ID is required.", false),
    CLIENT_SECRET("MS_CLIENT_SECRET", "MS Client Secret", "MS Client secret is required.", true),
    ERP_BRAND_KEY("ERP_BRAND_KEY", "ERP Brand Key", "Brand Key is required.", false),
    ERP_DISTRIBUTOR_BRAND_KEY("ERP_DISTRIBUTOR_BRAND_KEY", "ERP Distributor Brand Key", "Distributor Brand Key is required.", false),
    AMP_BUSINESS_NAME("AMP_BUSINESS_NAME", "AMP Business Name", "AMP Business Name is required.", false),
    AMP_DISTRIBUTOR_BUSINESS_NAME("AMP_DISTRIBUTOR_BUSINESS_NAME", "AMP Distributor Business Name",
            "AMP Distributor Business Name is required.", false);

    private final String inputName;
    private final String label;
    private final String requiredMessage;
    private final boolean secret;

    ErpField(String inputName, String label, String requiredMessage, boolean secret) {
        this.inputName = inputName;
        this.label = label;
        this.requiredMessage = requiredMessage;
        this.secret = secret;
    }

    public String inputName() {
        return inputName;
    }

    public String label() {
        return label;
    }

    public String requiredMessage() {
        return requiredMessage;
    }

    /** True for the password-type field, whose value must never be logged or asserted. */
    public boolean isSecret() {
        return secret;
    }
}
