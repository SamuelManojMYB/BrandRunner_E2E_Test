package ai.metayb.ui.wrappers;

import org.openqa.selenium.WebElement;

public interface TargetLocator {

    /**
     * This method will switch to the Window of interest
     * @param index The window index to be switched to. 0 -> first window 
     * @author Samuel Manoj
     */
    public void switchToWindow(int index);

    /**
     *
     * This method will switch to the specific frame
     * @param ele   - The Webelement (frame) to be switched
     * @author Samuel Manoj
     */
    public void switchToFrame(WebElement ele);

    /**
     * This method will accept the alert opened
     * @author Samuel Manoj
     */
    public void acceptAlert();

    /**
     * This method will dismiss the alert opened
     * @author Samuel Manoj
     */
    public void dismissAlert();

    /**
     * This method will return the text of the alert
     * @author Samuel Manoj
     */
    public String getAlertText();

    /**
     * Leaves any frame and returns to the top-level document.
     */
    public void switchToDefaultContent();

    /**
     * Moves from the current frame to its parent frame.
     */
    public void switchToParentFrame();

}
