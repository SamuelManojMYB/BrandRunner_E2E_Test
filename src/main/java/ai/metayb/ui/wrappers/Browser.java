package ai.metayb.ui.wrappers;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public interface Browser {

    /**
     * This method will launch and load the browser with maximized and timeout set
     * @param browser  - The name of the browser (chrome, firefox, safari, ie, edge)
     * @param url - The url of the application to be launched
     * @author Samuel Manoj
     * @return boolean - True if all loaded else false
     * @throws RuntimeException if browser driver setup fails
     */
    public boolean startApp(String browser, String url) ;

    /**
     * This method will locate the element using any given locator
     * @param locator  - The locator by which the element to be found
     * @param locValue - The locator value by which the element to be found
     * @author Samuel Manoj
     * @return WebElement - the first matching element
     * @throws org.openqa.selenium.NoSuchElementException if element not found
     */
    public WebElement locateElement(String locator, String locValue) ;


    /**
     * This method will verify browser actual title with expected
     * @param expectedTitle - The expected title of the browser
     * @author Samuel Manoj
     */
    public boolean verifyExactTitle(String expectedTitle);

    /**
     * This method will verify browser actual title with expected text using contains
     * @param expectedTitle - The expected title of the browser
     * @author Samuel Manoj
     */
    public boolean verifyPartialTitle(String expectedTitle);


    /**
     * This method will close the active browser
     * @author Samuel Manoj
     */
    public void closeActiveBrowser();

    /**
     * This method will close all the browsers
     * @author Samuel Manoj
     */
    public void closeAllBrowsers();

    /**
     * This method will quit the browser
     * @author Samuel Manoj
     */
    public void quitBrowser();

    /**
     * Explicit wait until the element is visible. Fails the step (screenshot + AssertionError) on timeout.
     * Use {@link BaseDriver#waitForElement} for the default 20 second visibility wait.
     * @param locator - The locator of the element to wait for
     * @param timeoutSeconds - Maximum time to wait, in seconds
     */
    public void waitForElementVisible(By locator, int timeoutSeconds);

    /**
     * Explicit wait until the element is visible and enabled. Fails the step on timeout.
     * @param locator - The locator of the element to wait for
     * @param timeoutSeconds - Maximum time to wait, in seconds
     */
    public void waitForElementClickable(By locator, int timeoutSeconds);

    /**
     * Explicit wait until the element is invisible or absent from the DOM. Fails the step on timeout.
     * @param locator - The locator of the element to wait for
     * @param timeoutSeconds - Maximum time to wait, in seconds
     */
    public void waitForElementInvisible(By locator, int timeoutSeconds);

    /** Same as {@link #waitForElementVisible(By, int)} with the default 20 second timeout. */
    public void waitForElementVisible(By locator);

    /** Same as {@link #waitForElementClickable(By, int)} with the default 20 second timeout. */
    public void waitForElementClickable(By locator);

    /** Same as {@link #waitForElementInvisible(By, int)} with the default 20 second timeout. */
    public void waitForElementInvisible(By locator);

    /**
     * Existence check that never fails the step: an absent element is a normal result (false), not an error.
     * No waiting is performed - combine with a wait method when the element renders asynchronously.
     * @param locator - The locator of the element to look for
     * @return true if at least one matching element is in the DOM (visible or not)
     */
    public boolean isPresent(By locator);

    /**
     * Navigates the current window to the given URL. Only the URL without query string/fragment is
     * written to logs and reports, since links can carry tokens.
     * @param url - The absolute URL to open
     */
    public void navigateToUrl(String url);

    /** Reloads the current page. */
    public void refresh();

    /** Navigates one step back in the browser history. */
    public void back();

    /** Navigates one step forward in the browser history. */
    public void forward();

    /**
     * @return the full current URL. Only the part without query string/fragment is written to logs and reports.
     */
    public String getCurrentUrl();

    /** Scrolls the window to the top of the page. */
    public void scrollToTop();

    /** Scrolls the window to the bottom of the page. */
    public void scrollToBottom();

}
