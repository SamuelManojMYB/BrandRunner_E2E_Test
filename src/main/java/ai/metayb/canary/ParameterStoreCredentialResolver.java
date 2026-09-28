package ai.metayb.canary;

import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

/**
 * Resolves Canary login credentials from AWS Systems Manager Parameter Store,
 * instead of Secrets Manager. Only invoked when CANARY_PASSWORD_PARAMETER is
 * actually configured (see CanaryConfig) - this class, and the AWS SDK
 * classes it touches, are never loaded during a local run that doesn't set
 * it, so local validation never needs AWS credentials just to exercise the
 * API flow.
 *
 * Unlike Secrets Manager's single JSON-blob secret, Parameter Store stores
 * one scalar value per parameter - so this expects two separate parameters
 * rather than one structured one:
 * <pre>
 * CANARY_PASSWORD_PARAMETER  -->  a SecureString parameter (required)
 * CANARY_USERNAME_PARAMETER  -->  a String parameter (optional - if unset,
 *                                  CanaryConfig falls back to the plain
 *                                  CANARY_USERNAME env var instead, since a
 *                                  login email isn't necessarily sensitive)
 * </pre>
 */
final class ParameterStoreCredentialResolver {

    private ParameterStoreCredentialResolver() {
    }

    /**
     * Fetches a single parameter's value, decrypting it if it's a SecureString.
     * withDecryption(true) is a no-op for a plain String parameter, so this is
     * safe to use for both the password and (optional) username parameter.
     */
    static String resolve(String parameterName) {
        try (SsmClient client = SsmClient.create()) {
            GetParameterResponse response = client.getParameter(
                    GetParameterRequest.builder().name(parameterName).withDecryption(true).build());

            String value = response.parameter().value();
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Parameter Store parameter '" + parameterName + "' is empty");
            }
            return value;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            // Deliberately logs only the exception type, never e.getMessage() verbatim -
            // AWS SDK exceptions here describe auth/permission/not-found failures, not
            // parameter content, but there's no reason to take that on faith in a canary log.
            throw new IllegalStateException(
                    "Failed to resolve Parameter Store parameter '" + parameterName + "': "
                            + e.getClass().getSimpleName(), e);
        }
    }
}
