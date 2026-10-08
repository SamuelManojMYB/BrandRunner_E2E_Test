package ai.metayb.ui.pages.onboarding;

import ai.metayb.ui.components.onboarding.CreateBusinessUnitOtpElements;
import ai.metayb.ui.components.onboarding.OtpFlow;
import ai.metayb.ui.wrappers.BaseDriver;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The OTP Verification section of the Create Business Unit dialog. Use {@link CreateBusinessUnitDialog#selectSection}
 * to bring the section on screen first. Only unsaved form state is touched: there is no Create operation here, and the
 * section has no OTP delivery or verification behavior, only four switches saying which flows will require an OTP.
 *
 * The switch input is invisible (opacity 0) and has no accessible name. It is found through the exact title of its row
 * and is never waited on for visibility: its state is read with {@code isSelected}, it is set with {@code check} /
 * {@code uncheck} (or a native click), and keys go straight to it.
 */
public class CreateBusinessUnitOtpSection extends BaseDriver {

    public static final String SECTION = "OTP Verification";

    private final CreateBusinessUnitOtpElements elements;

    public CreateBusinessUnitOtpSection() {
        super();
        elements = new CreateBusinessUnitOtpElements();
        PageFactory.initElements(getDriver(), elements);
    }

    // ---------------------------------------------------------------- static content

    public String getOtpHeading() {
        return getText(elements.getHeading()).trim();
    }

    public String getOtpDescription() {
        return getText(elements.getDescription()).trim();
    }

    public boolean isOtpSectionDisplayed() {
        return isDisplayed(elements.getHeading());
    }

    /** How many switches the section has. */
    public int getSwitchCount() {
        return elements.getSwitches().size();
    }

    /** The validation messages currently shown anywhere in the OTP section (there is no validation here). */
    public List<String> getVisibleErrors() {
        List<String> messages = new ArrayList<>();
        for (WebElement error : elements.getPanelErrors()) {
            messages.add(getText(error).trim());
        }
        return messages;
    }

    // ---------------------------------------------------------------- one flow

    /** Whether the flow's title is on screen. The title is checked, not the invisible switch input. */
    public boolean isFlowDisplayed(OtpFlow flow) {
        return isDisplayed(locateElement("xpath", CreateBusinessUnitOtpElements.titleXpath(flow)));
    }

    public String getFlowDescription(OtpFlow flow) {
        return getText(locateElement("xpath", CreateBusinessUnitOtpElements.descriptionXpath(flow))).trim();
    }

    public boolean isFlowEnabled(OtpFlow flow) {
        return isEnabled(flowSwitch(flow));
    }

    public boolean isFlowOn(OtpFlow flow) {
        return isSelected(flowSwitch(flow));
    }

    /** Switches the flow on, clicking only if it is off. */
    public void turnOn(OtpFlow flow) {
        check(flowSwitch(flow));
    }

    /** Switches the flow off, clicking only if it is on. */
    public void turnOff(OtpFlow flow) {
        uncheck(flowSwitch(flow));
    }

    /** Clicks the flow's switch once, whatever its state. */
    public void toggle(OtpFlow flow) {
        click(flowSwitch(flow));
    }

    /** Clicks the flow's title text, which the portal does not wire to the switch. */
    public void clickFlowTitle(OtpFlow flow) {
        click(locateElement("xpath", CreateBusinessUnitOtpElements.titleXpath(flow)));
    }

    // ---------------------------------------------------------------- keyboard

    /**
     * Puts keyboard focus on the flow's switch without changing it. WebDriver focuses an element before it sends keys,
     * so sending the "null" key focuses it and types nothing; a failure to take focus is reported.
     */
    public void focusFlow(OtpFlow flow) {
        typeWithoutClear(flowSwitch(flow), Keys.NULL.toString());
        if (!isFocused(flow)) {
            reportStep("Expected keyboard focus on the " + flow.title() + " switch but it is elsewhere", "FAIL");
        }
    }

    public boolean isFocused(OtpFlow flow) {
        return flowSwitch(flow).equals(getDriver().switchTo().activeElement());
    }

    /** Sends Space to the flow's switch, which toggles it. */
    public void toggleWithKeyboard(OtpFlow flow) {
        typeWithoutClear(flowSwitch(flow), Keys.SPACE.toString());
    }

    /** Sends Tab to the flow's switch, moving focus to the next control. */
    public void pressTab(OtpFlow flow) {
        typeWithoutClear(flowSwitch(flow), Keys.TAB.toString());
    }

    /** Sends Enter to the flow's switch, which the portal does not treat as a toggle. */
    public void pressEnter(OtpFlow flow) {
        typeWithoutClear(flowSwitch(flow), Keys.ENTER.toString());
    }

    // ----------------------------------------------------------------

    /** The raw switch input, found without any visibility wait. */
    private WebElement flowSwitch(OtpFlow flow) {
        return locateElement("xpath", CreateBusinessUnitOtpElements.switchXpath(flow));
    }
}
