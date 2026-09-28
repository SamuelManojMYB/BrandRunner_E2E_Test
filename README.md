# Introduction 
TODO: Give a short introduction of your project. Let this section explain the objectives or the motivation behind this project. 

# Getting Started
TODO: Guide users through getting your code up and running on their own system. In this section you can talk about:
1.	Installation process
2.	Software dependencies
3.	Latest releases
4.	API references

# Build and Test
TODO: Describe and show how to build your code and run the tests. 

# Contribute
TODO: Explain how other users and developers can contribute to make your code better. 

If you want to learn more about creating good readme files then refer the following [guidelines](https://docs.microsoft.com/en-us/azure/devops/repos/git/create-a-readme?view=azure-devops). You can also seek inspiration from the below readme files:
- [ASP.NET Core](https://github.com/aspnet/Home)
- [Visual Studio Code](https://github.com/Microsoft/vscode)
- [Chakra Core](https://github.com/Microsoft/ChakraCore)

# AWS CodeBuild + ECR (API automation)

The primary CI path for running the existing API TestNG suite
(`src/test/resources/testng/testng-api.xml`) in AWS: a Docker image built from
this repo, stored in Amazon ECR, executed by AWS CodeBuild. See
[docs/AWS_CODEBUILD.md](docs/AWS_CODEBUILD.md) for full setup instructions,
IAM requirements, and troubleshooting. UI/Mobile automation are unaffected -
this only runs the API suite.

# CloudWatch Synthetics Canary (legacy)

**Status: legacy.** AWS CodeBuild (above) is now the primary path for
automated/CI execution of the API suite. This Canary implementation is kept
as-is, not deleted, and not depended on by the CodeBuild setup - it remains
useful as a standalone periodic health check if you continue running it, but
new CI-style execution work should go through CodeBuild instead.

A lightweight, six-step API health/smoke journey (`ApiHealthCanary`) that runs
periodically through AWS CloudWatch Synthetics, reusing this project's existing
REST Assured framework. It is completely separate from the TestNG regression
suite - it never invokes a TestNG test, and `mvn clean test` is unaffected by
any of this.

## Architecture

```
CloudWatch Synthetics
        |
ApiHealthCanary            (ai.metayb.canary - new, main-scope)
        |
CanaryApiSessionClient      (ai.metayb.api.client - new, main-scope)
        |
REST Assured (SanitizedApiLoggingFilter attached, same as every TestNG API test)
        |
BrandRunners Web API
```

`CanaryApiSessionClient` is a small extraction of `testUtils.BaseApiTest`'s
login/request-spec logic into main-scope code - `BaseApiTest` itself is
untouched. See that class's Javadoc for exactly why it can't be reused
directly (it's test-scope, so it never ships in a packaged jar, and its auth
state is static/suite-scoped, which a Canary must not reuse).

## Canary steps

Sequential, because each step after Login needs the token/business_unit
Login produced. Every endpoint below is one already covered by a live-verified
TestNG test - see `ApiHealthCanary`'s per-step comments for exactly which one.

| # | Step | Endpoint | Validates |
|---|------|----------|-----------|
| 1 | Login | `POST /web/auth/login` | `accessToken` cookie + business unit id present |
| 2 | Current Session | `GET /web/session` | HTTP 200, `data.id`/`email`/`roleName`, non-empty `data.permissions` |
| 3 | Control Settings | `GET /web/settings/app-settings` | HTTP 200, `data.masterData.locationHierarchies`, `data.storedData.appSettings` |
| 4 | ANP Health | `GET /web/dashboard/counter` | HTTP 200, `data.totalUsers`, `data.totalTasks` |
| 5 | FWM Health | `GET /web/fwm-dashboard/status-summary?date=<today>&locationId=null&designationId=null` | HTTP 200, `data.statusSummary`. Date is computed fresh each run - never hardcoded. |
| 6 | Sales Health | `GET /web/sales-report/activity-lists` | HTTP 200, non-empty `data.details` |

**Known gap**: the actual `04. Sales` Postman folder has never been automated
or live-verified in this project, so there is no genuine "Sales" smoke
endpoint yet. Step 6 uses `03. FWM / Reports - Sales`'s `activity-lists`
endpoint as a verified stand-in. Revisit once `04. Sales` gets its own
automation pass.

## Handler

```
ai.metayb.canary.ApiHealthCanary::canaryCode
```

Verified against the SDK's actual current source (`aws/aws-cloudwatch-synthetics-sdk-java`
on GitHub) rather than AWS's own docs page, which shows a stale
`com.amazonaws.synthetics.Synthetics` import - the real, current package is
`software.amazon.synthetics`.

## Configuration

Layered, env vars taking priority over local config - never the other way
around:

```
CloudWatch env var  -->  existing local config (config.properties / credentials.properties)
```

| Variable | Required | Sensitive | Falls back to |
|---|---|---|---|
| `API_BASE_URL` | No | No | `config/config.properties`'s `api.base.url` |
| `API_TENANT` | No | No | `config/config.properties`'s `api.tenant` |
| `CANARY_USERNAME` | No | No (unless you'd rather keep it secret too - see below) | `CANARY_USERNAME_PARAMETER`'s Parameter Store value, then `config/credentials.properties`'s `apiEmail` |
| `CANARY_PASSWORD` | No | **Yes** | Only used if `CANARY_PASSWORD_PARAMETER` is not set |
| `CANARY_PASSWORD_PARAMETER` | No | No (it's a parameter name, not a secret itself) | If set, the password comes from this Parameter Store parameter instead of `CANARY_PASSWORD` |
| `CANARY_USERNAME_PARAMETER` | No | No | If set (and `CANARY_USERNAME` isn't), the username comes from this Parameter Store parameter too |

A real CloudWatch deployment should set `CANARY_PASSWORD_PARAMETER` (see below)
rather than `CANARY_PASSWORD` directly. `credentials.properties` is a
**local-dev-only** fallback - the deployed Canary artifact deliberately does
not include that file at all (see "Packaging" below), so in a real deployment
this fallback path is unreachable unless something is misconfigured, in which
case the Canary fails cleanly with a message telling you exactly which
variable is missing (never a stack trace with no explanation).

## Parameter Store

Preferred way to supply credentials in CloudWatch. Chosen over Secrets
Manager: this Canary needs one static credential with no rotation, so a plain
SSM parameter is the simpler, cheaper fit. Unlike Secrets Manager's single
JSON-blob secret, Parameter Store holds one scalar value per parameter, so
this uses two separate parameters:

| Parameter | Type | Required |
|---|---|---|
| Password parameter (name set via `CANARY_PASSWORD_PARAMETER`) | **SecureString** | Yes |
| Username parameter (name set via `CANARY_USERNAME_PARAMETER`) | String | No - omit it and set `CANARY_USERNAME` as a plain env var instead if the login email isn't considered sensitive |

Set `CANARY_PASSWORD_PARAMETER` to the password parameter's name (e.g.
`/brandrunner/canary/password`). Nothing in source code, `config.properties`,
`synthetics.json`, or the generated ZIP ever contains a real credential - see
"Security / logging validation" below for how that was verified.

## Logging

`ApiHealthCanary` points Log4j2 at a dedicated `log4j2-canary.xml`
(console-only) before any Logger is first obtained, instead of the existing
`log4j2.xml` used by every UI/API/mobile test. That file's `RollingFile`
appender writes to a relative `logs/` path that isn't writable (or visible in
CloudWatch Logs) inside the Synthetics Lambda runtime - reusing it as-is would
make every Canary run emit noisy "unable to create file" diagnostics for an
appender that can never do anything useful there. The existing `log4j2.xml`
is untouched and still applies to every other execution path.

`SanitizedApiLoggingFilter` (already used by every TestNG API test) is reused
unchanged - `Authorization`/`Cookie`/`Set-Cookie`/`cron-secret` headers and
secret-shaped JSON fields are masked exactly as they already are in the
regression suite.

## Build

```bash
mvn clean package -Pcanary
```

Default `mvn clean test` / `mvn clean package` are completely unaffected -
the canary jar/zip only get built when `-Pcanary` is passed explicitly.

## Generated artifact

```
target/canary.zip
```

Structure:

```
canary.zip
├── lib/
│   ├── end-to-end-testing-1.0-SNAPSHOT-canary.jar   (this project's own classes + a filtered
│   │                                                  resource set - see "Packaging" below)
│   └── ...runtime dependency jars actually needed (REST Assured, Jackson, Log4j2,
│         the AWS Systems Manager SDK and its own transitives)
└── synthetics.json
```

## Packaging

`config/credentials.properties` (a real, committed local-dev password) lives
in the same `src/main/resources` directory every other main-scope class
shares, and Maven's default jar packages that whole directory. Reusing the
project's normal jar as-is would therefore have shipped that file - and
several irrelevant UI/mobile binaries (`test-data/AutoIT/*.exe`, a sample
image, an Excel fixture) - inside the Canary artifact. Instead, the `canary`
Maven profile builds a **separate, filtered jar** (classifier `canary`) that
excludes:

- `config/credentials.properties`, `config/input.properties`, `config/mobile/**`
- `test-data/**`
- `log4j2.xml` (the Canary uses `log4j2-canary.xml` instead)
- `META-INF/services/**` (a Selenium-specific SPI registration, unrelated to the Canary)

`config/config.properties` (base URL/tenant - not a secret) is kept, since
`CanaryConfig`'s local-dev fallback path reads it.

The dependency jars in `lib/` are filtered too (`src/assembly/canary-zip.xml`),
since every dependency in this shared `pom.xml` - Selenium, Appium, Selenide,
WebDriverManager, POI, Cucumber, Allure, TestNG, ExtentReports - is declared
at ordinary "compile" scope (the whole project shares one `pom.xml` by
design), so Maven's own scope filtering can't tell "used by the Canary" apart
from "used by the Selenium tests." Every exclude in that file was verified
against a real `mvn dependency:tree` run to confirm it has no other consumer,
and the final `lib/` set was verified empirically (not just reasoned about) by
running the actual Canary logic with *only* those extracted jars on the
classpath - see "Local Canary validation" below.

## Local Canary validation

Runs the real `ApiHealthCanary.canaryCode(...)` against a `LocalSyntheticsStub`
(a minimal stand-in for the managed Synthetics runtime) - no CloudWatch/AWS
infrastructure required, unless you deliberately set `CANARY_PASSWORD_PARAMETER`
to test the Parameter Store path.

```bash
mvn -q dependency:build-classpath -Dmdep.outputFile=cp.txt -Dmdep.includeScope=test
java -cp "target/classes;target/test-classes;@cp.txt" ai.metayb.canary.local.LocalCanaryRunner
```

(or run `ai.metayb.canary.local.LocalCanaryRunner#main` directly from an IDE).
Both `LocalCanaryRunner` and `LocalSyntheticsStub` live under `src/test/java`
specifically so they can never end up in the packaged Canary artifact.

## CloudWatch setup (manual - not automated by this codebase)

1. **Create canary** in the CloudWatch Synthetics console.
2. **Select the Java runtime** (`syn-java-1.0`).
3. **Upload `target/canary.zip`** (built via `mvn clean package -Pcanary`) as the canary code, or reference it from S3.
4. **Set the handler** to `ai.metayb.canary.ApiHealthCanary::canaryCode`.
5. **Set environment variables**: `API_BASE_URL`, `API_TENANT`, `CANARY_PASSWORD_PARAMETER` (and `CANARY_USERNAME` if you're keeping the login email non-secret, or `CANARY_USERNAME_PARAMETER` if not).
6. **Grant Parameter Store access**: attach `ssm:GetParameter` (scoped to the specific parameter ARN(s)) and `kms:Decrypt` (for the SecureString parameter's KMS key) to the canary's execution role (see IAM below).
7. **Configure the execution role**: either let the console create one (it adds the standard Synthetics permissions automatically), or attach a role with the minimum permissions listed below.
8. **Configure the S3 artifact location** (a `cw-syn-results-*` bucket, or your own - the execution role needs `s3:PutObject` there).
9. **Run a test/dry run** and confirm all six steps pass in the step report before scheduling anything recurring.
10. **Configure the schedule** (e.g. every 5 or 15 minutes - this is a lightweight health check, not a load test).
11. **Enable the canary.**

## IAM - minimum permissions for the execution role

Do not use `AdministratorAccess`. Trust policy: `lambda.amazonaws.com` (canaries run as Lambda functions under the hood).

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:GetBucketLocation"],
      "Resource": "arn:aws:s3:::<your-canary-results-bucket>/*"
    },
    {
      "Effect": "Allow",
      "Action": ["logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"],
      "Resource": "arn:aws:logs:*:*:log-group:/aws/lambda/cwsyn-*"
    },
    {
      "Effect": "Allow",
      "Action": "cloudwatch:PutMetricData",
      "Resource": "*",
      "Condition": { "StringEquals": { "cloudwatch:namespace": "CloudWatchSynthetics" } }
    },
    {
      "Effect": "Allow",
      "Action": "ssm:GetParameter",
      "Resource": [
        "arn:aws:ssm:*:*:parameter/<your-canary-password-parameter-name>",
        "arn:aws:ssm:*:*:parameter/<your-canary-username-parameter-name>"
      ]
    },
    {
      "Effect": "Allow",
      "Action": "kms:Decrypt",
      "Resource": "arn:aws:kms:*:*:key/<key-id-or-alias-used-to-encrypt-the-SecureString-parameter>"
    }
  ]
}
```

The `kms:Decrypt` statement is required for the SecureString password parameter -
scope it to whichever KMS key actually encrypts it (a customer-managed key's
ARN, or the account's default `alias/aws/ssm` key). Only include the second
`ssm:GetParameter` resource entry if you're also using `CANARY_USERNAME_PARAMETER`.

(`xray:PutTraceSegments` on `*` too, only if X-Ray tracing is enabled for the canary.)

## Recommended CloudWatch alarms

- **Canary failure** - alarm on the `Failed` metric (per-canary, `CanaryName` dimension) being `> 0`.
- **Success percentage** - alarm on `SuccessPercent` dropping below a threshold (e.g. `< 100` for a few consecutive periods, to avoid alerting on one transient blip).
- **Step-level failures** - each step emits its own `SuccessPercent`/`Duration` (per `synthetics.json`'s `stepSuccessMetric`/`stepDurationMetric`); alarm on `Login`'s specifically if you want an early, more specific signal than the aggregate.
- **Duration** - alarm if total run duration trends up significantly over baseline; a slow-but-passing canary is often the earliest sign of real API degradation.

Not created automatically by this codebase - configure these in CloudWatch once the canary is running.