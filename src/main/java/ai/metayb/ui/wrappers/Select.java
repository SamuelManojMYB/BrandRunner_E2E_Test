package ai.metayb.ui.wrappers;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public interface Select {

    /**
     * This method will select the drop down visible text
     * @param ele   - The Webelement (dropdown) to be selected
     * @param value The value to be selected (visibletext) from the dropdown 
     * @author Samuel Manoj
     */
    public void selectDropDownUsingVisibleText(WebElement ele, String value) ;

    /**
     * This method will select the drop down using value
     * @param ele   - The Webelement (dropdown) to be selected
     * @param value The value attribute to be selected from the dropdown 
     * @author Samuel Manoj
     */
    public void selectDropDownUsingValue(WebElement ele, String value) ;

    /**
     * This method will select the drop down using index
     * @param ele   - The Webelement (dropdown) to be selected
     * @param index The index to be selected from the dropdown 
     * @author Samuel Manoj
     */
    public void selectDropDownUsingIndex(WebElement ele, int index) ;
    /**
     * This method will get the selected value from the dropdown
     * @param ele   - The Webelement (dropdown) to be selected
     * @author Samuel Manoj
     */
    public String getSelectedValue(WebElement ele) ;

    /**
     * This method will get the selected visible text from the dropdown
     * @param ele   - The Webelement (dropdown) to be selected
     * @author Samuel Manoj
     */
    public String getSelectedVisibleText(WebElement ele) ;

    /**
     * This method will get the selected index from the dropdown
     * @param ele   - The Webelement (dropdown) to be selected
     * @author Samuel Manoj
     */
    public int getSelectedIndex(WebElement ele) ;

    /**
     * This method will get the all options from the dropdown
     * @param ele   - The Webelement (dropdown) to be selected
     * @author Samuel Manoj
     */
    public int getAllOptions(WebElement ele) ;

    /**
     * Minimal support for non-native (e.g. Material UI) dropdowns: click the trigger, wait for the
     * options container to be visible, wait for the option to be clickable, then click it. The page
     * object owns the locators (the DOM differs per component); nothing is assumed about it here.
     * @param dropdown - The element that opens the dropdown
     * @param optionsContainer - Locator of the popup/list that holds the options
     * @param option - Locator of the exact option to pick (e.g. built from its visible text)
     */
    public void selectCustomDropdownOption(WebElement dropdown, By optionsContainer, By option);

    /**
     * Verifies what a custom dropdown currently shows: its visible text, or its "value" property when
     * it has no text (input-based dropdowns).
     * @param dropdown - The element that displays the selected value
     * @param expectedValue - The expected (trimmed) value
     */
    public void verifyCustomDropdownValue(WebElement dropdown, String expectedValue);

}
