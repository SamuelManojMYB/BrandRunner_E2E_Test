package ai.metayb.ui.components.onboarding;

/**
 * The four flows of the OTP Verification section, each with the exact title and description the portal shows next to
 * its switch. The switches have no name, id or accessible name, so the title is the only stable way to tell them apart.
 */
public enum OtpFlow {

    PICKUP("Pickup", "Require an OTP when stock is picked up."),
    DELIVERY("Delivery", "Require an OTP when an order is delivered."),
    MANUAL_SALE("Manual Sale", "Require an OTP to complete a manual sale."),
    RESTOCK_PICKUP("Restock Pickup", "Require an OTP when restock inventory is picked up.");

    private final String title;
    private final String description;

    OtpFlow(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }
}
