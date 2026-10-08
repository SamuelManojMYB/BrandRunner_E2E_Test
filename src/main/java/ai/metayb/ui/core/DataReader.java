package ai.metayb.ui.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DataReader {

    private static final Properties prop = new Properties();

    static {
        try (InputStream input = DataReader.class
                .getClassLoader()
                .getResourceAsStream("config/credentials.properties")) {

            if (input == null) {
                throw new RuntimeException(
                        "File not found in resources: config/credentials.properties"
                );
            }

            prop.load(input);

        } catch (IOException e) {
            throw new RuntimeException("Failed to load credentials.properties", e);
        }
    }

    public final String emailAddress = prop.getProperty("emailAddress");
    public final String password = prop.getProperty("password");

    public final String apiEmail = prop.getProperty("apiEmail");
    public final String apiPassword = prop.getProperty("apiPassword");

    public final String onboardingUiUsername = prop.getProperty("onboarding.ui.username");
    public final String onboardingUiPassword = prop.getProperty("onboarding.ui.password");
}
