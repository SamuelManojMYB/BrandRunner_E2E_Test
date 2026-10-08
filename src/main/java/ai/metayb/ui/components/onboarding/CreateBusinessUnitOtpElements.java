package ai.metayb.ui.components.onboarding;

import lombok.Getter;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

/**
 * Locators for the OTP Verification section of the Create Business Unit dialog, from the live DOM.
 *
 * The section is four rows, each a text block (title paragraph, description paragraph) beside a MUI Switch whose real
 * control is an {@code input[role=switch][type=checkbox]}. That input has no name, id, aria-label or wrapping label and
 * is rendered with {@code opacity: 0}, so WebDriver reports it as not displayed: it must be located and read without
 * waiting for visibility. Each switch is found through the exact title of its own row (title paragraph, its parent text
 * block, that block's parent row), which also keeps "Pickup" from matching "Restock Pickup". Every Create Business
 * Unit panel stays mounted (hidden), so everything is scoped to this panel through its heading. No generated ids.
 */
@Getter
public class CreateBusinessUnitOtpElements {

    private static final String DIALOG = DashboardPageElements.CREATE_DIALOG_XPATH;

    private static final String HEADING_XPATH = DIALOG + "//h6[normalize-space()='OTP Verification']";
    /** The panel: the box that holds the heading, the description and the four rows. */
    private static final String PANEL = HEADING_XPATH + "/parent::div";
    private static final String DESCRIPTION_XPATH =
            PANEL + "/p[normalize-space()='Choose which flows require OTP verification before they can be completed.']";
    private static final String PANEL_ERRORS_XPATH = PANEL + "//p[contains(@class,'Mui-error')]";
    private static final String SWITCHES_XPATH = PANEL + "//input[@role='switch']";

    @FindBy(xpath = HEADING_XPATH)
    private WebElement heading;

    @FindBy(xpath = DESCRIPTION_XPATH)
    private WebElement description;

    @FindBy(xpath = PANEL_ERRORS_XPATH)
    private List<WebElement> panelErrors;

    @FindBy(xpath = SWITCHES_XPATH)
    private List<WebElement> switches;

    // ---- locators that depend on a flow ----

    /** The row of a flow: found from its exact title, so the title block's parent is the row. */
    private static String rowXpath(OtpFlow flow) {
        return PANEL + "//p[normalize-space()=" + DashboardPageElements.xpathLiteral(flow.title()) + "]/parent::div/parent::div";
    }

    public static String titleXpath(OtpFlow flow) {
        return PANEL + "//p[normalize-space()=" + DashboardPageElements.xpathLiteral(flow.title()) + "]";
    }

    public static String descriptionXpath(OtpFlow flow) {
        return rowXpath(flow) + "//p[normalize-space()=" + DashboardPageElements.xpathLiteral(flow.description()) + "]";
    }

    /** The switch input of a flow. It is invisible (opacity 0), so never wait for its visibility. */
    public static String switchXpath(OtpFlow flow) {
        return rowXpath(flow) + "//input[@role='switch']";
    }
}
