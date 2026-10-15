package ai.metayb.ui.budget;

import io.qameta.allure.Allure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

/**
 * Keeps track of every backend record the Budget write tests create, so nothing is left unaccounted for. The tests
 * NEVER delete anything: created records are only logged (log, an Allure attachment and
 * target/budget-created-records.txt) and later tests in the same run reuse them. Nothing secret is recorded: a title,
 * a group id from the URL and a Request ID (budget code).
 */
public final class BudgetRecords {

    private static final Logger LOGGER = LogManager.getLogger(BudgetRecords.class);
    private static final Path FILE = Paths.get("target", "budget-created-records.txt");

    private static String draftTitle;
    private static String groupId;
    private static String requestCode;
    private static boolean submitted;

    private BudgetRecords() {
    }

    /** Records a created or changed record. {@code kind} is e.g. "pre-draft", "draft", "submitted". */
    public static synchronized void track(String testId, String kind, String title, String group, String code) {
        if (title != null) {
            draftTitle = title;
        }
        if (group != null) {
            groupId = group;
        }
        if (code != null) {
            requestCode = code;
        }
        if ("submitted".equals(kind)) {
            submitted = true;
        }
        String line = LocalDateTime.now() + " | " + testId + " | " + kind + " | title=" + title + " | groupId=" + group + " | requestId=" + code;
        LOGGER.info("[Budget record] {}", line);
        Allure.addAttachment("Created budget record (" + kind + ")", "text/plain", line);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.warn("[Budget record] could not append to {}: {}", FILE, e.getMessage());
        }
    }

    /**
     * Records a record created by a standalone test (outside the E2E write chain). It is logged and listed like any
     * other, but it does not become "the" draft the chain's later steps look for.
     */
    public static synchronized void trackStandalone(String testId, String kind, String title, String detail) {
        String line = LocalDateTime.now() + " | " + testId + " | " + kind + " | title=" + title + " | " + detail;
        LOGGER.info("[Budget record] {}", line);
        Allure.addAttachment("Created budget record (" + kind + ")", "text/plain", line);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOGGER.warn("[Budget record] could not append to {}: {}", FILE, e.getMessage());
        }
    }

    public static synchronized String draftTitle() {
        return draftTitle;
    }

    public static synchronized String groupId() {
        return groupId;
    }

    public static synchronized String requestCode() {
        return requestCode;
    }

    public static synchronized boolean isSubmitted() {
        return submitted;
    }
}
