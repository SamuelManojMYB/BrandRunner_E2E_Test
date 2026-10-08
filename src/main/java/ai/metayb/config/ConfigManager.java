package ai.metayb.config;

import ai.metayb.ui.core.DataReader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.function.Supplier;

public class ConfigManager {
    private static final Properties props = new Properties();

    static {
        loadConfig("config.properties");
    }

    private static void loadConfig(String fileName) {
        try (InputStream input = ConfigManager.class.getClassLoader()
                .getResourceAsStream("config/" + fileName)) {
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load configuration: " + fileName, e);
        }
    }

    // API Configurations
    // API_BASE_URL / API_TENANT env vars take precedence when present - this is what
    // lets the same unmodified jar run in a Docker/CodeBuild container against a
    // different environment than config.properties' checked-in default, without
    // touching that file. Falls back to config.properties for local runs, exactly as
    // before this override was added.
    public static String getApiBaseUrl() {
        return firstNonBlank(() -> System.getenv("API_BASE_URL"), () -> props.getProperty("api.base.url"));
    }

    public static String getApiTenant() {
        return firstNonBlank(() -> System.getenv("API_TENANT"), () -> props.getProperty("api.tenant"));
    }

    // API_USERNAME / API_PASSWORD let a container run (CodeBuild, or any `docker run`)
    // supply real credentials via environment variables - sourced from AWS Systems
    // Manager Parameter Store at the CodeBuild layer, never read from SSM by this
    // code directly - instead of the committed credentials.properties, which is
    // deliberately excluded from the Docker build context (see .dockerignore) and
    // must never contain real secrets baked into an image. Local runs with no
    // environment configured keep working exactly as before: DataReader is only
    // constructed lazily, as a fallback, if the env var is absent.
    public static String getApiUsername() {
        return firstNonBlank(() -> System.getenv("API_USERNAME"), ConfigManager::localApiEmail);
    }

    public static String getApiPassword() {
        return firstNonBlank(() -> System.getenv("API_PASSWORD"), ConfigManager::localApiPassword);
    }

    private static String localApiEmail() {
        return new DataReader().apiEmail;
    }

    private static String localApiPassword() {
        return new DataReader().apiPassword;
    }

    @SafeVarargs
    private static String firstNonBlank(Supplier<String>... suppliers) {
        for (Supplier<String> supplier : suppliers) {
            String value = supplier.get();
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    // UI Configurations
    public static String getBrowser() {
        return props.getProperty("ui.browser");
    }

    public static String getBaseUrl() {
        return props.getProperty("ui.url");
    }

    // UI_HEADLESS / UI_WINDOW_WIDTH / UI_WINDOW_HEIGHT: environment variable, then JVM system
    // property (-DUI_HEADLESS=false), then config.properties (ui.headless, ui.window.width,
    // ui.window.height). Unset means "keep BaseDriver's existing per-browser behavior" (Chrome
    // headless + maximized, other browsers headed + maximized), hence null rather than a default.
    public static Boolean getUiHeadless() {
        return parseHeadless(firstNonBlank(() -> System.getenv("UI_HEADLESS"),
                () -> System.getProperty("UI_HEADLESS"), () -> props.getProperty("ui.headless")), "UI_HEADLESS");
    }

    private static Boolean parseHeadless(String value, String settingName) {
        if (value == null) {
            return null;
        }
        if (value.trim().equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (value.trim().equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        throw new IllegalArgumentException(settingName + " must be 'true' or 'false'");
    }

    // Onboarding Portal UI. Precedence: ONBOARDING_UI_* environment variable, then JVM system
    // property (-Donboarding.ui.url=...), then the properties file, then the existing legacy UI value
    // where one exists. These never read or write the legacy ui.* / API settings above.
    // The password has no -D form on purpose: command-line arguments are visible in process lists
    // and CI logs, so it comes only from the environment or credentials.properties.
    private static final String DEFAULT_ONBOARDING_UI_URL = "https://qas-onboarding.brandrunners.ai/";

    public static String getOnboardingUiUrl() {
        return firstNonBlank(() -> System.getenv("ONBOARDING_UI_URL"),
                () -> System.getProperty("onboarding.ui.url"),
                () -> props.getProperty("onboarding.ui.url"),
                () -> DEFAULT_ONBOARDING_UI_URL);
    }

    public static String getOnboardingUiBrowser() {
        return firstNonBlank(() -> System.getenv("ONBOARDING_UI_BROWSER"),
                () -> System.getProperty("onboarding.ui.browser"),
                () -> props.getProperty("onboarding.ui.browser"),
                ConfigManager::getBrowser);
    }

    /** @return null when unset everywhere, which keeps BaseDriver's existing per-browser default. */
    public static Boolean getOnboardingUiHeadless() {
        String value = firstNonBlank(() -> System.getenv("ONBOARDING_UI_HEADLESS"),
                () -> System.getProperty("onboarding.ui.headless"),
                () -> props.getProperty("onboarding.ui.headless"));
        return value == null ? getUiHeadless() : parseHeadless(value, "ONBOARDING_UI_HEADLESS");
    }

    public static String getOnboardingUiUsername() {
        return firstNonBlank(() -> System.getenv("ONBOARDING_UI_USERNAME"),
                () -> System.getProperty("onboarding.ui.username"),
                ConfigManager::localOnboardingUsername);
    }

    public static String getOnboardingUiPassword() {
        return firstNonBlank(() -> System.getenv("ONBOARDING_UI_PASSWORD"),
                ConfigManager::localOnboardingPassword);
    }

    private static String localOnboardingUsername() {
        return new DataReader().onboardingUiUsername;
    }

    private static String localOnboardingPassword() {
        return new DataReader().onboardingUiPassword;
    }

    /** @return {width, height}, or null when neither is configured. */
    public static int[] getUiWindowSize() {
        String width = firstNonBlank(() -> System.getenv("UI_WINDOW_WIDTH"),
                () -> System.getProperty("UI_WINDOW_WIDTH"), () -> props.getProperty("ui.window.width"));
        String height = firstNonBlank(() -> System.getenv("UI_WINDOW_HEIGHT"),
                () -> System.getProperty("UI_WINDOW_HEIGHT"), () -> props.getProperty("ui.window.height"));
        if (width == null && height == null) {
            return null;
        }
        if (width == null || height == null) {
            throw new IllegalArgumentException("UI_WINDOW_WIDTH and UI_WINDOW_HEIGHT must be set together");
        }
        try {
            int w = Integer.parseInt(width.trim());
            int h = Integer.parseInt(height.trim());
            if (w <= 0 || h <= 0) {
                throw new NumberFormatException();
            }
            return new int[]{w, h};
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("UI_WINDOW_WIDTH and UI_WINDOW_HEIGHT must be positive integers");
        }
    }

    // Performance Configurations
    public static int getPerformanceThreads() {
        return Integer.parseInt(props.getProperty("performance.threads", "50"));
    }
}