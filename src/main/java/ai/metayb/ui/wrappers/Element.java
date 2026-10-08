package ai.metayb.ui.wrappers;

import org.openqa.selenium.WebElement;

public interface Element {

    /**
     * This method will enter the value in the given text field 
     * @param ele   - The Webelement (text field) in which the data to be entered
     * @param data  - The data to be sent to the webelement
     * @author Samuel Manoj
     * @throws org.openqa.selenium.ElementNotInteractableException if the element is not interactable (e.g., not visible or disabled)
     */
    public void type(WebElement ele, String data) ;

    /**
     * This method will enter the value in the given text field, exactly like {@link #type},
     * but without logging the value itself to the console/Allure report - use this for
     * passwords, tokens, or any other field whose value must never appear in a log or report.
     * @param ele   - The Webelement (text field) in which the data to be entered
     * @param data  - The sensitive data to be sent to the webelement
     * @author Samuel Manoj
     * @throws org.openqa.selenium.ElementNotInteractableException if the element is not interactable (e.g., not visible or disabled)
     */
    public void typeSecret(WebElement ele, String data);

    /**
     * Empties a text field with real key presses (one Backspace per character) instead of Selenium's clear().
     * Use this when the field must end up empty and the app has to notice: clear() changes only the DOM value,
     * and a React-controlled input then keeps its old state and never runs its validation. The value is not
     * logged. Fails the step if the field is not empty afterwards.
     * @param ele   - The Webelement (text field) to empty
     */
    public void clearField(WebElement ele);

    /**
     * Replaces whatever the field holds with the given text, for fields that already have a value and are
     * React-controlled. {@link #type} clears with Selenium's clear(), which React can undo within milliseconds
     * (it re-renders the field from its own state), so the new text gets appended to the old value instead.
     * This empties the field with key presses first ({@link #clearField}) and then types, so React's state and
     * the DOM agree throughout.
     * @param ele   - The Webelement (text field) to overwrite
     * @param data  - The new text
     */
    public void replaceText(WebElement ele, String data);

    /**
     * This method will enter the value in the given text field without clearing
     * @param ele   - The Webelement (text field) in which the data to be entered
     * @param data  - The data to be sent to the webelement
     * @author Samuel Manoj
     * @throws org.openqa.selenium.ElementNotInteractableException if the element is not interactable (e.g., not visible or disabled)
     */
    public void typeWithoutClear(WebElement ele, String data) ;

    /**
     * This method will enter the value in the given text field and Submit
     * @param ele   - The Webelement (text field) in which the data to be entered
     * @param data  - The data to be sent to the webelement
     * @author Samuel Manoj
     * @throws  org.openqa.selenium.ElementNotInteractableException if the element is not interactable (e.g., not visible or disabled)
     */
    public void typeAndEnter(WebElement ele, String data) ;

    /**
     * This method will click the element and take snap
     * @param ele   - The Webelement (button/link/element) to be clicked
     * @author Samuel Manoj
     */
    public void click(WebElement ele);

    /**
     * This method will get the text of the element
     * @param ele   - The Webelement (button/link/element) in which text to be retrieved
     * @author Samuel Manoj
     */
    public String getText(WebElement ele);

    /**
     * This method will verify exact given text with actual text on the given element
     * @param ele   - The Webelement in which the text to be need to be verified
     * @param expectedText  - The expected text to be verified
     * @author Samuel Manoj
     */
    public void verifyExactText(WebElement ele, String expectedText);

    /**
     * This method will verify given text contains actual text on the given element
     * @param ele   - The Webelement in which the text to be need to be verified
     * @param expectedText  - The expected text to be verified
     * @author Samuel Manoj
     */
    public void verifyPartialText(WebElement ele, String expectedText);

    /**
     * This method will verify exact given attribute's value with actual value on the given element
     * @param ele   - The Webelement in which the attribute value to be need to be verified
     * @param attribute  - The attribute to be checked (like value, href etc)
     * @param value  - The value of the attribute
     * @author Samuel Manoj
     */
    public void verifyExactAttribute(WebElement ele, String attribute, String value);

    /**
     * This method will verify partial given attribute's value with actual value on the given element
     * @param ele   - The Webelement in which the attribute value to be need to be verified
     * @param attribute  - The attribute to be checked (like value, href etc)
     * @param value  - The value of the attribute
     * @author Samuel Manoj
     */
    public void verifyPartialAttribute(WebElement ele, String attribute, String value);

    /**
     * This method will verify if the element (Radio button, Checkbox)  is selected
     * @param ele   - The Webelement (Radio button, Checkbox) to be verified
     * @author Samuel Manoj
     */
    public void verifySelected(WebElement ele);

    /**
     * This method will verify if the element is visible in the DOM
     * @param ele   - The Webelement to be checked
     * @author Samuel Manoj
     */
    public void verifyDisplayed(WebElement ele);

    /**
     * State query that never fails the step: an element that is absent or stale is reported as false.
     * (Use {@link #verifyDisplayed} when absence should fail the test.)
     * @param ele - The Webelement to check
     * @return true if the element is present and displayed
     */
    public boolean isDisplayed(WebElement ele);

    /**
     * State query that never fails the step: an element that is absent or stale is reported as false.
     * @param ele - The Webelement to check
     * @return true if the element is present and enabled
     */
    public boolean isEnabled(WebElement ele);

    /**
     * State query that never fails the step: an element that is absent or stale is reported as false.
     * Also serves as "is checked" for checkboxes and radio buttons.
     * @param ele - The Webelement (checkbox, radio, option) to check
     * @return true if the element is present and selected/checked
     */
    public boolean isSelected(WebElement ele);

    /**
     * Returns the live DOM property if the browser exposes one (e.g. the current "value" of an input),
     * otherwise the attribute as written in the HTML. The value is masked in logs/reports for sensitive fields
     * (judged by type, name, id, autocomplete, aria-label and placeholder, so a password revealed as plain text is
     * still masked) and for sensitive-looking attribute names, but is still returned to the caller.
     * @param ele - The Webelement to read
     * @param attribute - The attribute/property name (like value, href, aria-label)
     * @return the value, or null if the element has neither
     */
    public String getAttribute(WebElement ele, String attribute);

    /**
     * Scrolls the element into the middle of the viewport (also scrolls any scrollable ancestor).
     * @param ele - The Webelement to bring into view
     */
    public void scrollToElement(WebElement ele);

    /**
     * Native click first. ONLY if the browser reports the click as intercepted or not interactable does
     * it retry once with a JavaScript click, and the fallback is recorded as a distinct step so it is
     * never silent. Any other failure (stale/missing element, ...) fails the step as {@link #click} does.
     * Prefer {@link #click}; use this only for a documented, understood interception problem.
     * @param ele - The Webelement to click
     */
    public void clickWithJsFallback(WebElement ele);

    /**
     * Ensures a checkbox is checked. Reads the state first and clicks only if it is unchecked,
     * then verifies the new state.
     * @param ele - The checkbox input
     */
    public void check(WebElement ele);

    /**
     * Ensures a checkbox is unchecked. Reads the state first and clicks only if it is checked,
     * then verifies the new state.
     * @param ele - The checkbox input
     */
    public void uncheck(WebElement ele);

    /**
     * Ensures a radio button is selected. Clicks only if it is not already selected, then verifies.
     * @param ele - The radio input
     */
    public void selectRadio(WebElement ele);

}
