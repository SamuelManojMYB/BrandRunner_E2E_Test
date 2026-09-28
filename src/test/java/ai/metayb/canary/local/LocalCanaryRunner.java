package ai.metayb.canary.local;

import ai.metayb.canary.ApiHealthCanary;

import java.util.concurrent.ExecutionException;

/**
 * Runs the real ApiHealthCanary.canaryCode(...) logic locally against a
 * LocalSyntheticsStub, without any CloudWatch/AWS Synthetics infrastructure.
 *
 * This validates exactly what Section 10 asks for: configuration loading,
 * login, token propagation, all six steps in sequence, exception handling,
 * logging, and (by inspecting the console output afterward) that nothing
 * sensitive got logged. It reuses the production handler class directly -
 * this is not a reimplementation or a mock of the Canary's own logic, only of
 * the Synthetics runtime around it.
 *
 * Needs no AWS credentials UNLESS CANARY_PASSWORD_PARAMETER is set in the
 * environment, in which case Parameter Store genuinely is being exercised on
 * purpose - see README "Local Canary validation" for both modes.
 *
 * Usage:
 *   mvn test-compile exec:java -Dexec.mainClass=ai.metayb.canary.local.LocalCanaryRunner -Dexec.classpathScope=test
 * or run main() directly from an IDE.
 */
public final class LocalCanaryRunner {

    private LocalCanaryRunner() {
    }

    public static void main(String[] args) {
        System.out.println("=== Local Canary validation starting ===");
        try {
            new ApiHealthCanary().canaryCode(new LocalSyntheticsStub());
            System.out.println("=== Local Canary validation PASSED ===");
        } catch (Throwable t) {
            Throwable cause = (t instanceof ExecutionException && t.getCause() != null) ? t.getCause() : t;
            System.out.println("=== Local Canary validation FAILED: " + cause + " ===");
            System.exit(1);
        }
    }
}
