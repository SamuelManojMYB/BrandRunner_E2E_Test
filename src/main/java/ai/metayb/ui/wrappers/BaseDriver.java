package ai.metayb.ui.wrappers;

import ai.metayb.config.ConfigManager;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import io.qameta.allure.model.Status;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class BaseDriver implements Browser, Element, Select, TargetLocator {

    static {
        // Selenium's default "jdk-http-client" factory fails to construct on this
        // machine (java.net.http.HttpClient's internal Selector/Pipe loopback
        // wakeup uses a Unix Domain Socket that this machine's network/security
        // stack rejects - reproduced for both Chrome and Edge, across every local
        // JDK 17/21/24 build). ai.metayb.selenium.UrlConnectionHttpClient avoids
        // that entirely via plain java.net.HttpURLConnection. See its Javadoc.
        System.setProperty("webdriver.http.factory", "urlconnection");
    }

    private static final ThreadLocal<RemoteWebDriver> driverThreadLocal = new ThreadLocal<>();
    private static final int DEFAULT_WAIT_SECONDS = 20;
    protected Logger logger;

    public BaseDriver() {
        this.logger = LogManager.getLogger(this.getClass());
    }

    @Override
    public boolean startApp(String browser, String url) {
        return startApp(browser, url, ConfigManager::getUiHeadless);
    }

    /**
     * Same as {@link #startApp(String, String)} but with the headless setting supplied by the caller
     * (e.g. {@code ConfigManager::getOnboardingUiHeadless}). It is resolved inside the launch so an
     * invalid value is reported like any other launch failure.
     */
    public boolean startApp(String browser, String url, Supplier<Boolean> headless) {
        try {
            // Unset (null) keeps the pre-existing behavior: Chrome headless, every other browser headed.
            Boolean headlessSetting = headless.get();
            int[] windowSize = ConfigManager.getUiWindowSize();
            RemoteWebDriver newDriver;
            switch (browser.toLowerCase()) {
                case "chrome":
                    WebDriverManager.chromedriver().setup();
                    ChromeOptions chromeOptions = new ChromeOptions();
                    if (headlessSetting == null || headlessSetting) {
                        chromeOptions.addArguments("--headless");
                    }
                    if (windowSize != null) {
                        chromeOptions.addArguments("--window-size=" + windowSize[0] + "," + windowSize[1]);
                    }
                    chromeOptions.addArguments("--no-sandbox");
                    chromeOptions.addArguments("--disable-dev-shm-usage");
                    chromeOptions.setAcceptInsecureCerts(true); // for HTTPS issues
                    chromeOptions.addArguments("--remote-allow-origins=*");
                    // Isolated profile per run so this doesn't collide with an already-running
                    // Chrome instance on the same machine (shared default profile/singleton lock).
                    chromeOptions.addArguments("--user-data-dir=" +
                            java.nio.file.Files.createTempDirectory("chrome-profile-").toAbsolutePath());
                    newDriver = new ChromeDriver(chromeOptions);
                    break;
                case "firefox":
                    WebDriverManager.firefoxdriver().setup();
                    FirefoxOptions firefoxOptions = new FirefoxOptions();
                    if (Boolean.TRUE.equals(headlessSetting)) {
                        firefoxOptions.addArguments("-headless");
                    }
                    newDriver = new FirefoxDriver(firefoxOptions);
                    break;
                case "ie":
                    WebDriverManager.iedriver().setup();
                    newDriver = new InternetExplorerDriver();
                    break;
                case "edge":
                    WebDriverManager.edgedriver().setup();
                    EdgeOptions options = new EdgeOptions();
                    if (Boolean.TRUE.equals(headlessSetting)) {
                        options.addArguments("--headless");
                    }
                    if (windowSize != null) {
                        options.addArguments("--window-size=" + windowSize[0] + "," + windowSize[1]);
                    }
                    newDriver = new EdgeDriver(options);
                    break;
                default:
                    System.err.println("This browser " + browser + " is not supported");
                    return false;
            }
            driverThreadLocal.set(newDriver);
            newDriver.get(url);
            if (windowSize != null) {
                newDriver.manage().window().setSize(new Dimension(windowSize[0], windowSize[1]));
            } else {
                newDriver.manage().window().maximize();
            }
            reportStep("The browser " + browser + " launched successfully", "PASS");
            return true;
        } catch (Exception e) {
            reportStep("The browser " + browser + " could not be launched: " + e.getMessage(), "FAIL");
            return false;
        }
    }

    public static RemoteWebDriver getDriver() {
        return driverThreadLocal.get();
    }

    public void waitForElement(By ele) {
        try {
            WebDriverWait wait = new WebDriverWait(getDriver(), Duration.ofSeconds(DEFAULT_WAIT_SECONDS));
            wait.until(ExpectedConditions.visibilityOfElementLocated(ele));
            reportStep("Waited for the element to be present", "PASS");
        } catch (Exception e) {
            reportStep("Could not wait for the element to be present: " + e.getMessage(), "FAIL");
        }
    }

    private void waitUntil(ExpectedCondition<?> condition, int timeoutSeconds, String what) {
        try {
            new WebDriverWait(getDriver(), Duration.ofSeconds(timeoutSeconds)).until(condition);
            reportStep("Waited for " + what, "PASS");
        } catch (Exception e) {
            reportStep("Could not wait for " + what + " within " + timeoutSeconds + "s: " + e.getMessage(), "FAIL");
        }
    }

    @Override
    public void waitForElementVisible(By locator, int timeoutSeconds) {
        waitUntil(ExpectedConditions.visibilityOfElementLocated(locator), timeoutSeconds, "the element to be visible");
    }

    @Override
    public void waitForElementClickable(By locator, int timeoutSeconds) {
        waitUntil(ExpectedConditions.elementToBeClickable(locator), timeoutSeconds, "the element to be clickable");
    }

    @Override
    public void waitForElementInvisible(By locator, int timeoutSeconds) {
        waitUntil(ExpectedConditions.invisibilityOfElementLocated(locator), timeoutSeconds, "the element to be invisible");
    }

    @Override
    public void waitForElementVisible(By locator) {
        waitForElementVisible(locator, DEFAULT_WAIT_SECONDS);
    }

    @Override
    public void waitForElementClickable(By locator) {
        waitForElementClickable(locator, DEFAULT_WAIT_SECONDS);
    }

    @Override
    public void waitForElementInvisible(By locator) {
        waitForElementInvisible(locator, DEFAULT_WAIT_SECONDS);
    }

    @Override
    public WebElement locateElement(String locator, String locValue) {
        try {
            switch (locator.toLowerCase()) {
                case "id":
                    return getDriver().findElement(By.id(locValue));
                case "name":
                    return getDriver().findElement(By.name(locValue));
                case "class":
                    return getDriver().findElement(By.className(locValue));
                case "xpath":
                    return getDriver().findElement(By.xpath(locValue));
                case "css":
                    return getDriver().findElement(By.cssSelector(locValue));
                case "linktext":
                    return getDriver().findElement(By.linkText(locValue));
                case "partiallinktext":
                    return getDriver().findElement(By.partialLinkText(locValue));
                case "tagname":
                    return getDriver().findElement(By.tagName(locValue));
                default:
                    reportStep("The locator " + locator + " is not supported", "FAIL");
                    return null;
            }
        } catch (NoSuchElementException e) {
            reportStep("The element with locator: " + locator + ", value: " + locValue + " was not found", "FAIL");
            return null;
        }
    }

    @Override
    public boolean verifyExactTitle(String expectedTitle) {
        String actualTitle = getDriver().getTitle();
        if (actualTitle.equals(expectedTitle)) {
            reportStep("The title " + expectedTitle + " is verified", "PASS");
            return true;
        }
        reportStep("The title " + expectedTitle + " is not verified. Actual: " + actualTitle, "FAIL");
        return false;
    }

    @Override
    public boolean verifyPartialTitle(String expectedTitle) {
        String actualTitle = getDriver().getTitle();
        if (actualTitle.contains(expectedTitle)) {
            reportStep("The title " + expectedTitle + " is verified", "PASS");
            return true;
        }
        reportStep("The title " + expectedTitle + " is not verified. Actual: " + actualTitle, "FAIL");
        return false;
    }

    @Override
    public void closeActiveBrowser() {
        try {
            getDriver().close();
            reportStep("The active browser is closed", "PASS");
        } catch (Exception e) {
            reportStep("The active browser could not be closed: " + e.getMessage(), "FAIL");
        } finally {
            driverThreadLocal.remove();
        }
    }

    @Override
    public void closeAllBrowsers() {
        try {
            getDriver().quit();
            reportStep("All browsers are closed", "PASS");
        } catch (Exception e) {
            reportStep("All browsers could not be closed: " + e.getMessage(), "FAIL");
        } finally {
            driverThreadLocal.remove();
        }
    }

    @Override
    public void quitBrowser() {
        try {
            getDriver().quit();
            reportStep("The browser is quit", "PASS");
        } catch (Exception e) {
            reportStep("The browser could not be quit: " + e.getMessage(), "FAIL");
        } finally {
            driverThreadLocal.remove();
        }
    }

    @Override
    public void switchToWindow(int index) {
        try {
            Set<String> windowHandles = getDriver().getWindowHandles();
            List<String> allWindows = new ArrayList<>(windowHandles);
            getDriver().switchTo().window(allWindows.get(index));
            reportStep("The window of index " + index + " switched", "PASS");
        } catch (Exception e) {
            reportStep("The window of index " + index + " could not be switched: " + e.getMessage(), "FAIL");
        }
    }

    @Override
    public void switchToFrame(WebElement ele) {
        try {
            getDriver().switchTo().frame(ele);
            reportStep("Switched to the frame", "PASS");
        } catch (Exception e) {
            reportStep("Could not switch to the frame: " + e.getMessage(), "FAIL");
        }
    }

    @Override
    public void acceptAlert() {
        try {
            getDriver().switchTo().alert().accept();
            reportStep("Alert accepted", "PASS");
        } catch (Exception e) {
            reportStep("Alert could not be accepted: " + e.getMessage(), "FAIL");
        }
    }

    @Override
    public void dismissAlert() {
        try {
            getDriver().switchTo().alert().dismiss();
            reportStep("Alert dismissed", "PASS");
        } catch (Exception e) {
            reportStep("Alert could not be dismissed: " + e.getMessage(), "FAIL");
        }
    }

    @Override
    public String getAlertText() {
        try {
            String text = getDriver().switchTo().alert().getText();
            reportStep("Alert text: " + text, "INFO");
            return text;
        } catch (Exception e) {
            reportStep("Could not get alert text: " + e.getMessage(), "FAIL");
            return "";
        }
    }

    @Attachment(value = "Screenshot", type = "image/png")
    public byte[] attachScreenshot(String stepDesc) {
        if(getDriver() != null) {
            try {
                // A revealed secret would be readable in the picture, so put the dots back first.
                SensitiveData.maskRevealedSecrets(getDriver());
                File src = getDriver().getScreenshotAs(OutputType.FILE);
                byte[] screenshot = getDriver().getScreenshotAs(OutputType.BYTES);
                Allure.addAttachment(stepDesc, new ByteArrayInputStream(screenshot));
                return FileUtils.readFileToByteArray(src);
            } catch (IOException e) {
                return new byte[0];
            }
        } else {
            System.out.println("Driver is null. Skipping screenshot.");
            return new byte[0];
        }
    }

    public void reportStep(String stepDesc, String status) {
        logger.info("[Test] - {}", stepDesc);
        if (status.equalsIgnoreCase("PASS")) {
            Allure.step(stepDesc, Status.PASSED);
        } else if (status.equalsIgnoreCase("FAIL")) {
            attachScreenshot(stepDesc);
            Allure.step(stepDesc, Status.FAILED);
            throw new AssertionError(stepDesc);
        } else {
            Allure.step(stepDesc);
        }
    }

    @Override
    public void type(WebElement ele, String data) {
        String shown = displayedData(ele, data);
        try {
            ele.clear();
            ele.sendKeys(data);
            reportStep("The data " + shown + " is entered", "PASS");
        } catch (Exception e) {
            reportStep("The data " + shown + " could not be entered: " + e.getMessage(), "FAIL");
        }
    }

    /**
     * What to show in a log line or Allure step for text sent to this element: the text itself, or a mask when the
     * element is sensitive (see {@link #isSensitiveElement}). Decided before sending, so a failure message is masked too.
     */
    private String displayedData(WebElement ele, String data) {
        return isSensitiveElement(ele) ? SensitiveData.MASK : data;
    }

    /**
     * Whether this element is a sensitive field. Judged by its current type and by its name, id, autocomplete,
     * aria-label and placeholder, never by the type alone: revealing a password turns it into a text input.
     * Any attribute that cannot be read is simply ignored.
     */
    protected boolean isSensitiveElement(WebElement ele) {
        try {
            return SensitiveData.isSensitiveField(ele.getDomAttribute("type"),
                    ele.getDomAttribute("name"), ele.getDomAttribute("id"), ele.getDomAttribute("autocomplete"),
                    ele.getDomAttribute("aria-label"), ele.getDomAttribute("placeholder"));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void typeSecret(WebElement ele, String data) {
        try {
            ele.clear();
            ele.sendKeys(data);
            reportStep("A masked value is entered", "PASS");
        } catch (Exception e) {
            reportStep("A masked value could not be entered: " + e.getMessage(), "FAIL");
        }
    }

    @Override
    public void clearField(WebElement ele) {
        try {
            String current = ele.getDomProperty("value");
            int length = current == null ? 0 : current.length();
            if (length > 0) {
                ele.sendKeys(Keys.END);
                ele.sendKeys(Keys.BACK_SPACE.toString().repeat(length));
            }
            String remaining = ele.getDomProperty("value");
            if (remaining != null && !remaining.isEmpty()) {
                reportStep("The field could not be cleared: " + remaining.length() + " character(s) remain", "FAIL");
            }
            reportStep("The field is cleared", "PASS");
        } catch (Exception e) {
            reportStep("The field could not be cleared: " + e.getMessage(), "FAIL");
        }
    }

    @Override
    public void replaceText(WebElement ele, String data) {
        clearField(ele);
        typeWithoutClear(ele, data);
    }

    @Override
    public void typeWithoutClear(WebElement ele, String data) {
        String shown = displayedData(ele, data);
        try {
            ele.sendKeys(data);
            reportStep("The data " + shown + " is entered", "PASS");
        } catch (Exception e) {
            reportStep("The data " + shown + " could not be entered: " + e.getMessage(), "FAIL");
        }
    }

    @Override
    public void typeAndEnter(WebElement ele, String data) {
        String shown = displayedData(ele, data);
        try {
            ele.clear();
            ele.sendKeys(data, Keys.ENTER);
            reportStep("The data " + shown + " is entered and submitted", "PASS");
        } catch (Exception e) {
            reportStep("The data " + shown + " could not be entered and submitted: " + e.getMessage(), "FAIL");
        }
    }

    @Override
    public void click(WebElement ele) {
        try {
            ele.click();
            reportStep("The element is clicked", "PASS");
        } catch (Exception e) {
            reportStep("The element could not be clicked: " + e.getMessage(), "FAIL");
        }
    }

    @Override
    public String getText(WebElement ele) {
        try {
            String text = ele.getText();
            reportStep("The text is: " + text, "INFO");
            return text;
        } catch (Exception e) {
            reportStep("Could not get the text: " + e.getMessage(), "FAIL");
            return "";
        }
    }

    @Override
    public void verifyExactText(WebElement ele, String expectedText) {
        String actualText = ele.getText();
        if (actualText.equals(expectedText)) {
            reportStep("The text " + expectedText + " is verified", "PASS");
        } else {
            reportStep("The text " + expectedText + " is not verified", "FAIL");
        }
    }

    @Override
    public void verifyPartialText(WebElement ele, String expectedText) {
        String actualText = ele.getText();
        if (actualText.contains(expectedText)) {
            reportStep("The text " + expectedText + " is verified", "PASS");
        } else {
            reportStep("The text " + expectedText + " is not verified", "FAIL");
        }
    }

    @Override
    public void verifyExactAttribute(WebElement ele, String attribute, String value) {
        String actualValue = ele.getDomAttribute(attribute);
        if (value.equals(actualValue)) {
            reportStep("The attribute " + attribute + " with value " + value + " is verified", "PASS");
        } else {
            reportStep("The attribute " + attribute + " with value " + value + " is not verified. Actual: " + actualValue, "FAIL");
        }
    }

    @Override
    public void verifyPartialAttribute(WebElement ele, String attribute, String value) {
        String actualValue = ele.getDomAttribute(attribute);
        if (actualValue != null && actualValue.contains(value)) {
            reportStep("The attribute " + attribute + " with value " + value + " is verified", "PASS");
        } else {
            reportStep("The attribute " + attribute + " with value " + value + " is not verified. Actual: " + actualValue, "FAIL");
        }
    }

    @Override
    public void verifySelected(WebElement ele) {
        if (ele.isSelected()) {
            reportStep("The element is selected", "PASS");
        } else {
            reportStep("The element is not selected", "FAIL");
        }
    }

    @Override
    public void verifyDisplayed(WebElement ele) {
        if (ele.isDisplayed()) {
            reportStep("The element is displayed", "PASS");
        } else {
            reportStep("The element is not displayed", "FAIL");
        }
    }

    @Override
    public void selectDropDownUsingVisibleText(WebElement ele, String value) {
        try {
            new org.openqa.selenium.support.ui.Select(ele).selectByVisibleText(value);
            reportStep("The dropdown is selected with visible text: " + value, "PASS");
        } catch (Exception e) {
            reportStep("The dropdown could not be selected with visible text: " + value + " (" + e.getMessage() + ")", "FAIL");
        }
    }

    @Override
    public void selectDropDownUsingValue(WebElement ele, String value) {
        try {
            new org.openqa.selenium.support.ui.Select(ele).selectByValue(value);
            reportStep("The dropdown is selected with value: " + value, "PASS");
        } catch (Exception e) {
            reportStep("The dropdown could not be selected with value: " + value + " (" + e.getMessage() + ")", "FAIL");
        }
    }

    @Override
    public void selectDropDownUsingIndex(WebElement ele, int index) {
        try {
            new org.openqa.selenium.support.ui.Select(ele).selectByIndex(index);
            reportStep("The dropdown is selected with index: " + index, "PASS");
        } catch (Exception e) {
            reportStep("The dropdown could not be selected with index: " + index + " (" + e.getMessage() + ")", "FAIL");
        }
    }

    @Override
    public String getSelectedValue(WebElement ele) {
        try {
            String selectedValue = new org.openqa.selenium.support.ui.Select(ele).getFirstSelectedOption().getAttribute("value");
            reportStep("The selected value is: " + selectedValue, "INFO");
            return selectedValue;
        } catch (Exception e) {
            reportStep("Could not get the selected value: " + e.getMessage(), "FAIL");
            return "";
        }
    }

    @Override
    public String getSelectedVisibleText(WebElement ele) {
        try {
            String selectedText = new org.openqa.selenium.support.ui.Select(ele).getFirstSelectedOption().getText();
            reportStep("The selected visible text is: " + selectedText, "INFO");
            return selectedText;
        } catch (Exception e) {
            reportStep("Could not get the selected visible text: " + e.getMessage(), "FAIL");
            return "";
        }
    }

    @Override
    public int getSelectedIndex(WebElement ele) {
        try {
            org.openqa.selenium.support.ui.Select select = new org.openqa.selenium.support.ui.Select(ele);
            int selectedIndex = select.getOptions().indexOf(select.getFirstSelectedOption());
            reportStep("The selected index is: " + selectedIndex, "INFO");
            return selectedIndex;
        } catch (Exception e) {
            reportStep("Could not get the selected index: " + e.getMessage(), "FAIL");
            return 0;
        }
    }

    @Override
    public int getAllOptions(WebElement ele) {
        try {
            int allOptions = new org.openqa.selenium.support.ui.Select(ele).getOptions().size();
            reportStep("The total number of options is: " + allOptions, "INFO");
            return allOptions;
        } catch (Exception e) {
            reportStep("Could not get the total number of options: " + e.getMessage(), "FAIL");
            return 0;
        }
    }

    public void assertPopupMessage(String expectedMessage) {
        try {
            WebElement toast = getDriver().findElement(By.xpath("//div[text()='" + expectedMessage + "']"));
            if (toast.isDisplayed()) {
                reportStep("Popup message is displayed: " + expectedMessage, "PASS");
            } else {
                reportStep("Popup message is not displayed: " + expectedMessage, "FAIL");
            }
        } catch (NoSuchElementException e) {
            reportStep("Popup message not found: " + expectedMessage, "FAIL");
        }
    }

    public void validateDisplayText(String actualText, String expectedText, String stepDesc) {
        if (actualText.equals(expectedText)) {
            reportStep(stepDesc + ": Text matched -> " + actualText, "PASS");
        } else {
            reportStep(stepDesc + ": Expected '" + expectedText + "' but found '" + actualText + "'", "FAIL");
        }
    }

    // ---- Existence / element state (queries: an absent element is a result, never a failure) ----

    @Override
    public boolean isPresent(By locator) {
        try {
            boolean present = !getDriver().findElements(locator).isEmpty();
            reportStep("The element is " + (present ? "present" : "not present"), "INFO");
            return present;
        } catch (Exception e) {
            reportStep("Could not check whether the element is present: " + e.getMessage(), "FAIL");
            return false;
        }
    }

    private boolean queryState(WebElement ele, Predicate<WebElement> state, String stateName) {
        try {
            boolean result = state.test(ele);
            reportStep("The element is " + (result ? "" : "not ") + stateName, "INFO");
            return result;
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            reportStep("The element is not " + stateName + " (element not found)", "INFO");
            return false;
        } catch (Exception e) {
            reportStep("Could not check whether the element is " + stateName + ": " + e.getMessage(), "FAIL");
            return false;
        }
    }

    @Override
    public boolean isDisplayed(WebElement ele) {
        return queryState(ele, WebElement::isDisplayed, "displayed");
    }

    @Override
    public boolean isEnabled(WebElement ele) {
        return queryState(ele, WebElement::isEnabled, "enabled");
    }

    @Override
    public boolean isSelected(WebElement ele) {
        return queryState(ele, WebElement::isSelected, "selected");
    }

    @Override
    public String getAttribute(WebElement ele, String attribute) {
        try {
            String value = ele.getDomProperty(attribute);
            if (value == null) {
                value = ele.getDomAttribute(attribute);
            }
            boolean masked = SensitiveData.isSensitiveName(attribute) || isSensitiveElement(ele);
            reportStep("The attribute " + attribute + " is: " + (masked ? SensitiveData.MASK : value), "INFO");
            return value;
        } catch (Exception e) {
            reportStep("Could not get the attribute " + attribute + ": " + e.getMessage(), "FAIL");
            return null;
        }
    }

    // ---- Navigation (URLs are logged without query string/fragment: links can carry tokens) ----

    private static String redactUrl(String url) {
        if (url == null) {
            return "null";
        }
        String withoutUserInfo = url.replaceFirst("//[^/?#@]*@", "//");
        int cut = withoutUserInfo.length();
        for (char c : new char[]{'?', '#'}) {
            int i = withoutUserInfo.indexOf(c);
            if (i >= 0 && i < cut) {
                cut = i;
            }
        }
        return withoutUserInfo.substring(0, cut);
    }

    // Keeps the browser's own reason (e.g. net::ERR_NAME_NOT_RESOLVED) but never a URL, which could carry a token.
    private static String firstLineWithoutUrls(String message) {
        if (message == null || message.isBlank()) {
            return "no message";
        }
        return message.lines().findFirst().orElse("").replaceAll("[a-zA-Z][a-zA-Z0-9+.-]*://\\S+", "<url>");
    }

    @Override
    public void navigateToUrl(String url) {
        try {
            getDriver().get(url);
            reportStep("Navigated to " + redactUrl(url), "PASS");
        } catch (Exception e) {
            reportStep("Could not navigate to " + redactUrl(url) + " (" + e.getClass().getSimpleName()
                    + ": " + firstLineWithoutUrls(e.getMessage()) + ")", "FAIL");
        }
    }

    @Override
    public void refresh() {
        try {
            getDriver().navigate().refresh();
            reportStep("The page is refreshed", "PASS");
        } catch (Exception e) {
            reportStep("The page could not be refreshed: " + e.getMessage(), "FAIL");
        }
    }

    @Override
    public void back() {
        try {
            getDriver().navigate().back();
            reportStep("Navigated back", "PASS");
        } catch (Exception e) {
            reportStep("Could not navigate back: " + e.getMessage(), "FAIL");
        }
    }

    @Override
    public void forward() {
        try {
            getDriver().navigate().forward();
            reportStep("Navigated forward", "PASS");
        } catch (Exception e) {
            reportStep("Could not navigate forward: " + e.getMessage(), "FAIL");
        }
    }

    @Override
    public String getCurrentUrl() {
        try {
            String url = getDriver().getCurrentUrl();
            reportStep("The current URL is: " + redactUrl(url), "INFO");
            return url;
        } catch (Exception e) {
            reportStep("Could not get the current URL: " + e.getMessage(), "FAIL");
            return "";
        }
    }

    // ---- Scrolling ----

    private void runScript(String script, Object... args) {
        ((JavascriptExecutor) getDriver()).executeScript(script, args);
    }

    @Override
    public void scrollToElement(WebElement ele) {
        try {
            runScript("arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});", ele);
            reportStep("Scrolled to the element", "PASS");
        } catch (Exception e) {
            reportStep("Could not scroll to the element: " + e.getMessage(), "FAIL");
        }
    }

    @Override
    public void scrollToTop() {
        try {
            runScript("window.scrollTo(0, 0);");
            reportStep("Scrolled to the top of the page", "PASS");
        } catch (Exception e) {
            reportStep("Could not scroll to the top of the page: " + e.getMessage(), "FAIL");
        }
    }

    @Override
    public void scrollToBottom() {
        try {
            runScript("window.scrollTo(0, document.documentElement.scrollHeight);");
            reportStep("Scrolled to the bottom of the page", "PASS");
        } catch (Exception e) {
            reportStep("Could not scroll to the bottom of the page: " + e.getMessage(), "FAIL");
        }
    }

    // ---- Click fallback, checkbox / radio ----

    @Override
    public void clickWithJsFallback(WebElement ele) {
        try {
            ele.click();
            reportStep("The element is clicked", "PASS");
            return;
        } catch (ElementNotInteractableException e) { // also covers ElementClickInterceptedException
            logger.warn("[Test] - Native click was blocked ({}); retrying once with a JavaScript click", e.getClass().getSimpleName());
            reportStep("Native click was blocked (" + e.getClass().getSimpleName()
                    + ") - using the JavaScript click fallback", "INFO");
        } catch (Exception e) {
            reportStep("The element could not be clicked: " + e.getMessage(), "FAIL");
        }
        try {
            runScript("arguments[0].click();", ele);
            reportStep("The element is clicked via the JavaScript fallback", "PASS");
        } catch (Exception e) {
            reportStep("The element could not be clicked, even via the JavaScript fallback: " + e.getMessage(), "FAIL");
        }
    }

    private void ensureSelected(WebElement ele, boolean desired, String what) {
        String target = desired ? "selected" : "cleared";
        try {
            if (ele.isSelected() == desired) {
                reportStep("The " + what + " is already " + target + " - no click needed", "INFO");
                return;
            }
            ele.click();
            if (ele.isSelected() != desired) {
                reportStep("The " + what + " could not be " + target + " - its state did not change after the click", "FAIL");
            }
            reportStep("The " + what + " is " + target, "PASS");
        } catch (Exception e) {
            reportStep("The " + what + " could not be " + target + ": " + e.getMessage(), "FAIL");
        }
    }

    @Override
    public void check(WebElement ele) {
        ensureSelected(ele, true, "checkbox");
    }

    @Override
    public void uncheck(WebElement ele) {
        ensureSelected(ele, false, "checkbox");
    }

    @Override
    public void selectRadio(WebElement ele) {
        ensureSelected(ele, true, "radio button");
    }

    // ---- Custom (non-native) dropdowns ----

    @Override
    public void selectCustomDropdownOption(WebElement dropdown, By optionsContainer, By option) {
        click(dropdown);
        waitForElementVisible(optionsContainer);
        waitForElementClickable(option);
        click(getDriver().findElement(option));
    }

    @Override
    public void verifyCustomDropdownValue(WebElement dropdown, String expectedValue) {
        String actual = dropdown.getText().trim();
        if (actual.isEmpty()) {
            String value = dropdown.getDomProperty("value");
            actual = value == null ? "" : value.trim();
        }
        if (actual.equals(expectedValue)) {
            reportStep("The dropdown value " + expectedValue + " is verified", "PASS");
        } else {
            reportStep("The dropdown value " + expectedValue + " is not verified. Actual: " + actual, "FAIL");
        }
    }

    // ---- Frames ----

    @Override
    public void switchToDefaultContent() {
        try {
            getDriver().switchTo().defaultContent();
            reportStep("Switched to the default content", "PASS");
        } catch (Exception e) {
            reportStep("Could not switch to the default content: " + e.getMessage(), "FAIL");
        }
    }

    @Override
    public void switchToParentFrame() {
        try {
            getDriver().switchTo().parentFrame();
            reportStep("Switched to the parent frame", "PASS");
        } catch (Exception e) {
            reportStep("Could not switch to the parent frame: " + e.getMessage(), "FAIL");
        }
    }
}
