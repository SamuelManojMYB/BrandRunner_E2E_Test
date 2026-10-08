package ai.metayb.ui.wrappers;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One place that decides what counts as sensitive, so logging, Allure steps, page source and screenshots all agree.
 *
 * A field is sensitive when its current {@code type} is password <em>or</em> any of its identifying attributes (name,
 * id, autocomplete, aria-label, placeholder) names a secret. The second rule matters: a show/hide toggle flips a
 * password input to {@code type=text}, and the portal's React inputs also reflect the typed text in the DOM
 * {@code value} attribute, so judging by the current type alone would leak the value as soon as it is revealed.
 */
public final class SensitiveData {

    /** Placeholder shown instead of a sensitive value. */
    public static final String MASK = "[masked]";

    /**
     * Words that mark an attribute or a field as sensitive. Matching is case-insensitive and by substring, so
     * currentPassword, new_password, PAYSTACK_SECRET_KEY, apiKey, x-access-key, Authorization and OTP all match.
     */
    private static final String NAME_REGEX =
            "(password|passwd|pwd|passcode|secret|token|api[-_ ]?key|access[-_ ]?key|private[-_ ]?key|authorization"
                    + "|cookie|credential|otp|one[-_ ]?time)";
    private static final Pattern NAME = Pattern.compile(NAME_REGEX, Pattern.CASE_INSENSITIVE);

    private static final Pattern INPUT_TAG = Pattern.compile("<input\\b(?:\"[^\"]*\"|'[^']*'|[^'\">])*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern IDENTIFYING_ATTRIBUTE = Pattern.compile(
            "(?<![\\w-])(type|name|id|autocomplete|aria-label|placeholder)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUE_ATTRIBUTE = Pattern.compile(
            "(?<![\\w-])value\\s*=\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s>]+)", Pattern.CASE_INSENSITIVE);

    /**
     * Turns any sensitive input that is currently shown as plain text back into a password input, so a screenshot
     * taken afterwards shows dots. It only runs on failure paths, where the page is about to be abandoned.
     */
    private static final String MASK_REVEALED_SECRETS_SCRIPT =
            "const re = new RegExp(arguments[0], 'i'); let changed = 0;"
                    + "document.querySelectorAll('input').forEach(function (i) {"
                    + "  const t = (i.getAttribute('type') || 'text').toLowerCase();"
                    + "  if (t === 'password') { return; }"
                    + "  const ids = [i.getAttribute('name'), i.id, i.getAttribute('autocomplete'), i.getAttribute('aria-label'), i.getAttribute('placeholder')];"
                    + "  if (ids.some(function (x) { return x && re.test(x); })) { i.setAttribute('type', 'password'); changed++; }"
                    + "});"
                    + "return changed;";

    private SensitiveData() {
    }

    /** True when this attribute or identifier names a secret (password, token, otp, ...). */
    public static boolean isSensitiveName(String name) {
        return name != null && NAME.matcher(name).find();
    }

    /**
     * True when a field with this type and these identifying attributes must be treated as sensitive. Null values are
     * ignored, so callers can pass whatever attributes they managed to read.
     */
    public static boolean isSensitiveField(String type, String... identifiers) {
        if (type != null && type.trim().equalsIgnoreCase("password")) {
            return true;
        }
        if (identifiers != null) {
            for (String identifier : identifiers) {
                if (isSensitiveName(identifier)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns the page source with the {@code value} of every sensitive input replaced by {@link #MASK}. Everything
     * else is untouched, so the source stays useful for diagnosing a failure.
     */
    public static String scrubPageSource(String html) {
        if (html == null || html.isEmpty()) {
            return html;
        }
        Matcher tags = INPUT_TAG.matcher(html);
        StringBuilder out = new StringBuilder(html.length());
        while (tags.find()) {
            String tag = tags.group();
            tags.appendReplacement(out, Matcher.quoteReplacement(isSensitiveTag(tag) ? maskValue(tag) : tag));
        }
        tags.appendTail(out);
        return out.toString();
    }

    /**
     * Re-masks sensitive inputs that are shown as plain text. Never throws: diagnostics must not fail a test run.
     *
     * @return how many inputs were switched back to password, or 0 when nothing changed or the script could not run
     */
    public static int maskRevealedSecrets(WebDriver driver) {
        try {
            if (driver instanceof JavascriptExecutor) {
                Object changed = ((JavascriptExecutor) driver).executeScript(MASK_REVEALED_SECRETS_SCRIPT, NAME_REGEX);
                return changed instanceof Number ? ((Number) changed).intValue() : 0;
            }
        } catch (Exception ignored) {
            // best effort only
        }
        return 0;
    }

    private static boolean isSensitiveTag(String tag) {
        String type = null;
        boolean named = false;
        Matcher attributes = IDENTIFYING_ATTRIBUTE.matcher(tag);
        while (attributes.find()) {
            String key = attributes.group(1);
            String value = attributes.group(2) != null ? attributes.group(2)
                    : attributes.group(3) != null ? attributes.group(3) : attributes.group(4);
            if ("type".equalsIgnoreCase(key)) {
                type = value;
            } else if (isSensitiveName(value)) {
                named = true;
            }
        }
        return named || isSensitiveField(type);
    }

    private static String maskValue(String tag) {
        return VALUE_ATTRIBUTE.matcher(tag).replaceAll(Matcher.quoteReplacement("value=\"" + MASK + "\""));
    }
}
