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

    // Performance Configurations
    public static int getPerformanceThreads() {
        return Integer.parseInt(props.getProperty("performance.threads", "50"));
    }
}