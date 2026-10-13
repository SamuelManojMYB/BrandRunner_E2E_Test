import java.io.File;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Checks a TestNG results file (target/surefire-reports/testng-results.xml): the report must be valid and
 * self-consistent, nothing in it may have failed, and at least one test must have actually RUN. Run by
 * docker/run-tests.sh as a single-file program (java CheckReport.java <file>), so it needs nothing beyond the JDK
 * already in the image.
 *
 * Report format (checked against real reports of this project, including a failed run): the root element
 * {@code testng-results} has the attributes total, passed, failed, skipped and ignored, and
 * total = passed + failed + skipped + ignored. "total" therefore also counts tests that a group selection or
 * enabled=false filtered out ("ignored"), so total alone says nothing about what ran.
 * "Executed" means passed + failed: skipped and ignored tests did not run.
 *
 * The root "failed" counts failed TEST methods only. A failed configuration method (e.g. a @BeforeClass login) is
 * not in it - Maven still fails the build, but it would be invisible if Maven were told to ignore failures - so
 * every {@code test-method} element with status FAIL is counted as well, configuration or not.
 *
 * Prints a one-line summary (counts only, never test data) and exits:
 *   0  the report is consistent, nothing failed, and at least one test executed
 *   1  the report is valid but a test or configuration method failed, or total is 0, or no test executed
 *      (zero-test, filtered-out or all-skipped run)
 *   2  the report is missing, not parseable, lacks the expected attributes, has inconsistent counts, or the XML
 *      parser could not be locked down (the check then fails closed and nothing is read)
 */
public class CheckReport {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("usage: CheckReport <testng-results.xml>");
            System.exit(2);
        }
        File file = new File(args[0]);
        if (!file.isFile()) {
            System.out.println("report not found: " + args[0]);
            System.exit(2);
        }

        DocumentBuilder builder;
        try {
            builder = secureBuilder();
        } catch (Exception e) {
            // Fail closed: if any security setting cannot be applied, the file is not parsed at all.
            System.out.println("XML security settings could not be applied (" + e.getClass().getSimpleName()
                    + "); the report was not read");
            System.exit(2);
            return;
        }

        int total, passed, failed, skipped, ignored, failedMethods = 0, failedConfig = 0;
        try {
            Element root = builder.parse(file).getDocumentElement();
            if (!"testng-results".equals(root.getTagName())) {
                System.out.println("report is not a TestNG results file (root element '" + root.getTagName() + "')");
                System.exit(2);
            }
            total = count(root, "total");
            passed = count(root, "passed");
            failed = count(root, "failed");
            skipped = count(root, "skipped");
            ignored = count(root, "ignored");
            if (total != passed + failed + skipped + ignored) {
                System.out.println("report counts are inconsistent: total=" + total + " but passed+failed+skipped+ignored="
                        + (passed + failed + skipped + ignored));
                System.exit(2);
            }
            NodeList methods = root.getElementsByTagName("test-method");
            for (int i = 0; i < methods.getLength(); i++) {
                Element method = (Element) methods.item(i);
                if ("FAIL".equals(method.getAttribute("status"))) {
                    if ("true".equals(method.getAttribute("is-config"))) {
                        failedConfig++;
                    } else {
                        failedMethods++;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("report is invalid: " + e.getClass().getSimpleName());
            System.exit(2);
            return;
        }

        int executed = passed + failed;
        String summary = "total=" + total + " executed=" + executed + " passed=" + passed + " failed=" + failed
                + " skipped=" + skipped + " ignored=" + ignored + " failed-configuration-methods=" + failedConfig;
        if (failed > 0 || failedMethods > 0 || failedConfig > 0) {
            System.out.println(summary + " - the report contains failures");
            System.exit(1);
        }
        if (total <= 0 || executed <= 0) {
            System.out.println(summary + " - no test was executed");
            System.exit(1);
        }
        System.out.println(summary);
    }

    /**
     * A parser that can read only the report itself: no DOCTYPE at all (so no entities and no external DTD), no
     * external DTD or schema access, no XInclude, no entity expansion, and an entity resolver that refuses anything.
     * Every setting is mandatory: an unsupported one throws, which the caller turns into a failed check.
     */
    private static DocumentBuilder secureBuilder() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setValidating(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> {
            throw new SAXException("external resources are not allowed");
        });
        return builder;
    }

    private static int count(Element root, String attribute) {
        String value = root.getAttribute(attribute);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing attribute " + attribute);
        }
        int number = Integer.parseInt(value.trim());
        if (number < 0) {
            throw new IllegalArgumentException("negative " + attribute);
        }
        return number;
    }
}
