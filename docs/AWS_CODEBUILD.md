# AWS CodeBuild + ECR: BrandRunners API Automation

Runs the existing, unmodified "BrandRunners API Suite" (`src/test/resources/testng/testng-api.xml`)
inside a Docker container, built and executed by AWS CodeBuild, with the image
stored in Amazon ECR for reuse. This replaces the need to run the suite from a
developer machine or a CloudWatch Synthetics Canary for CI-style execution.

**Scope: API automation by default, plus the Onboarding Portal UI suite on request.**
The image's default (`TEST_TARGET=api`) is exactly the original API-only behaviour.
UI runs (headless Chrome, section 21) are opt-in with `TEST_TARGET=ui` or `all`.
Mobile (Appium) automation is untouched, continues to compile as part of the same
Maven module, and is never invoked by this Docker image.

**Status of the CloudWatch Synthetics Canary** (`ai.metayb.canary.*`,
`ai.metayb.api.client.CanaryApiSessionClient`, `synthetics.json`,
`src/assembly/canary-zip.xml`, the `canary` Maven profile): kept as-is, not
deleted, not depended on by anything in this document. Treat it as **legacy** -
CodeBuild is the new path for automated/CI execution going forward. See the
existing "CloudWatch Synthetics Canary" section further down in this repo's
[README.md](../README.md) for that implementation's own docs, which remain
accurate for as long as the Canary itself is still deployed.

---

## 1. Prerequisites

- Docker Desktop (or another local Docker engine) - only needed for local
  build/run validation before pushing to AWS.
- AWS CLI v2, configured with credentials that can push to ECR (for the manual
  local ECR push steps below - CodeBuild itself uses its own service role, not
  your local AWS CLI credentials).
- An AWS account with permission to create: an ECR repository, a CodeBuild
  project, an IAM role, and (if not already present) two SSM Parameter Store
  SecureString parameters.
- This repository, with the changes in this document already merged/available
  to whatever CodeBuild source connection you configure.

Placeholders used throughout this document - substitute your own values, never
commit real ones:

| Placeholder | Meaning |
|---|---|
| `<AWS_ACCOUNT_ID>` | Target AWS account ID |
| `<AWS_REGION>` | e.g. `ap-south-1` |
| `<ECR_REPOSITORY>` | `brandrunner-api-automation` (suggested name) |
| `<CODEBUILD_ROLE_ARN>` | ARN of the IAM role CodeBuild assumes |
| `<IMAGE_TAG>` | An immutable tag - see section 9 |

---

## 2. Build the Docker image locally

```bash
docker build -t brandrunner-api-automation:local .
```

The `Dockerfile` is a two-stage build (dependency-resolution stage, then a
source-copy + compile stage) using `maven:3.9-eclipse-temurin-21`, matching
`pom.xml`'s `maven.compiler.source`/`target` of 21. See the Dockerfile's own
comments for why the final stage keeps a full Maven+JDK toolchain rather than a
slimmed-down JRE-only runtime (the container's entrypoint runs `mvn test`
directly - the same documented command below - so Maven must be present at
container *run* time, not just at image-build time).

`credentials.properties` is excluded from the build context by `.dockerignore` -
the image never contains real credentials.

## 3. Run the API suite locally in Docker

Determine the exact Maven command first (verified against `pom.xml` and
`testng-api.xml`, not assumed):

```bash
mvn clean test -DsuiteXmlFile=src/test/resources/testng/testng-api.xml
```

This is exactly what the image's `ENTRYPOINT` runs. To run it in a container:

```bash
docker run --rm \
  -e API_BASE_URL="https://maggie.qas.brandrunners.ai/api" \
  -e API_TENANT="maggie.qas" \
  -e API_USERNAME="<qa-username>" \
  -e API_PASSWORD="<qa-password>" \
  brandrunner-api-automation:local
```

These four environment variable names are real, existing, verified names - not
invented for this document:

- `API_BASE_URL` / `API_TENANT` - already read by `CanaryConfig` for the Canary;
  now also read by `ConfigManager.getApiBaseUrl()`/`getApiTenant()`, with the
  same env-var-first-then-`config.properties` precedence.
- `API_USERNAME` / `API_PASSWORD` - **new** in this change, added to
  `ConfigManager` (see section 5 below) specifically so containerized runs never
  need `credentials.properties`. Deliberately different from the Canary's
  `CANARY_USERNAME`/`CANARY_PASSWORD`, so the two execution paths stay fully
  independent.

A non-zero container exit code means the suite failed (Maven/Surefire's normal
behavior - nothing in this setup suppresses failures).

To copy the TestNG/Surefire reports out of a container after a run:

```bash
CID=$(docker create \
  -e API_BASE_URL="https://maggie.qas.brandrunners.ai/api" \
  -e API_TENANT="maggie.qas" \
  -e API_USERNAME="<qa-username>" \
  -e API_PASSWORD="<qa-password>" \
  brandrunner-api-automation:local)
docker start -a "$CID"; EXIT_CODE=$?
docker cp "$CID":/app/target/surefire-reports ./target/surefire-reports
docker rm "$CID" > /dev/null
exit $EXIT_CODE
```

This is exactly the pattern `buildspec.yml`/`buildspec-test-only.yml` use in
CodeBuild.

## 4. Create the ECR repository

```bash
aws ecr create-repository \
  --repository-name brandrunner-api-automation \
  --region <AWS_REGION> \
  --image-tag-mutability IMMUTABLE
```

(`IMMUTABLE` tags reinforce the immutable-tagging approach in section 9 - not
strictly required, but recommended.)

## 5. Authenticate Docker to ECR (manual, local push)

```bash
aws ecr get-login-password --region <AWS_REGION> \
  | docker login --username AWS --password-stdin <AWS_ACCOUNT_ID>.dkr.ecr.<AWS_REGION>.amazonaws.com
```

## 6. Build, tag, push (manual, local)

```bash
docker build -t brandrunner-api-automation:<IMAGE_TAG> .
docker tag brandrunner-api-automation:<IMAGE_TAG> \
  <AWS_ACCOUNT_ID>.dkr.ecr.<AWS_REGION>.amazonaws.com/brandrunner-api-automation:<IMAGE_TAG>
docker push \
  <AWS_ACCOUNT_ID>.dkr.ecr.<AWS_REGION>.amazonaws.com/brandrunner-api-automation:<IMAGE_TAG>
```

In CodeBuild this is automated by `buildspec.yml` (section 8).

## 7. Create the CodeBuild project (manual, AWS Console or CLI)

Not automated by this change - you'll do this yourself, per the instruction not
to create AWS resources automatically. Key settings:

- **Source**: this repository (whatever source connection type you use -
  CodeConnections/GitHub, CodeCommit, S3, etc.)
- **Environment image**: a CodeBuild-managed image with Docker support (e.g.
  "Amazon Linux 2 - Standard" or "aws/codebuild/standard:7.0"). The environment
  must be **x86_64 / AMD64** (a Linux x86 compute type, not an ARM/Graviton one):
  the image bundles a pinned Chrome for Testing, which Google publishes for
  linux64 (x86_64) only, and the Docker build refuses to continue on any other
  architecture (section 21).
- **Privileged mode**: **must be enabled** - CodeBuild runs its own Docker
  daemon inside the build environment to build/push/run images; without
  privileged mode, every `docker` command in `buildspec.yml` fails.
- **Service role**: a new or existing role with the permissions in section 11.
- **Buildspec**: use `buildspec.yml` from this repo (or `buildspec-test-only.yml`
  for a second, pull-and-run-only project - see section 9's two-stage note).
- **Environment variables**: see section 8's table (plain vars + the
  `parameter-store` secrets CodeBuild injects automatically from `buildspec.yml`
  - no manual SSM wiring needed in the Console beyond the parameters existing).

## 8. Required CodeBuild environment variables

Set directly on the CodeBuild project (Console: Environment > Additional
configuration > Environment variables), as plaintext (none of these are
secrets):

| Variable | Example value | Purpose |
|---|---|---|
| `AWS_ACCOUNT_ID` | `<AWS_ACCOUNT_ID>` | Used to construct the ECR registry URL |
| `AWS_DEFAULT_REGION` | `<AWS_REGION>` | ECR/SSM region |
| `ECR_REPOSITORY` | `brandrunner-api-automation` | Repository name (not the full URL) |
| `API_BASE_URL` | `https://maggie.qas.brandrunners.ai/api` | Target environment |
| `API_TENANT` | `maggie.qas` | Target tenant |

`buildspec-test-only.yml` additionally reads `IMAGE_TAG` (which already-pushed
tag to pull and run; defaults to `latest` if unset).

`API_USERNAME`/`API_PASSWORD` are **not** set here - they come from SSM
Parameter Store via `buildspec.yml`'s `env.parameter-store` block (section 12).

For a project that uses **`buildspec-ui.yml`** (UI or combined runs, section 21), set the same variables as
`buildspec-test-only.yml` plus these (all plaintext, none secret; full descriptions in section 21):

| Variable | Example value | Purpose |
|---|---|---|
| `TEST_TARGET` | `ui` | `api`, `ui` or `all`; this buildspec defaults to `ui` when unset |
| `UI_GROUPS` | `smoke` | Optional: narrow the UI suite to existing TestNG group(s); empty = whole UI suite |
| `ONBOARDING_UI_URL` | `https://qas-onboarding.brandrunners.ai/` | Optional: portal to test; empty = the URL in `config.properties` |

`ONBOARDING_UI_USERNAME`/`ONBOARDING_UI_PASSWORD` are likewise **not** set here; they come from SSM (section 12).

## 9. Immutable image tagging

Both buildspec files tag/push using `$CODEBUILD_RESOLVED_SOURCE_VERSION` (the
git commit SHA CodeBuild resolves for the triggering source revision) as the
primary, immutable tag, in addition to `latest`:

```yaml
IMAGE_TAG="${CODEBUILD_RESOLVED_SOURCE_VERSION:-manual-$(date +%Y%m%d%H%M%S)}"
```

This means every pushed image is independently reproducible by commit SHA, while
`latest` still exists for convenience (e.g. `buildspec-test-only.yml`'s default).

### Two-stage alternative (build vs. run), and which one to use

Rebuilding the full image on every test execution means re-resolving Maven
dependencies and recompiling the entire shared module (UI/mobile included) each
time - not free, though Docker layer caching within a single CodeBuild build
(and `dependency:go-offline`'s separate layer) keeps repeat builds on an
unchanged `pom.xml` reasonably fast.

**Recommendation**: start with the single `buildspec.yml` flow (build + push +
run in one CodeBuild project) - it's the simplest to set up and keeps every test
execution automatically validated against the latest source. If/when test
executions become frequent enough that rebuilding on every run is a real cost or
time concern, split into two CodeBuild projects:

- **Image build project** - triggered on source changes (e.g. push to `main`),
  runs `buildspec.yml`.
- **Test execution project** - triggered on-demand or on a schedule (see section
  20), runs `buildspec-test-only.yml`, which pulls the most recently pushed
  image (`IMAGE_TAG` defaulting to `latest`, or pinned to a specific commit SHA)
  and only runs it - no build/push phase at all.

This repo already includes both buildspec files so this split is a
configuration change (point a second CodeBuild project at
`buildspec-test-only.yml`), not a code change.

## 10. Exact CodeBuild execution flow (`buildspec.yml`)

```
Source checkout
  -> aws ecr get-login-password | docker login   (pre_build)
  -> docker build .                               (build)
  -> docker tag  :<commit-sha>  and  :latest       (build)
  -> docker push :<commit-sha>  and  :latest       (post_build)
  -> docker create + docker start -a (runs `mvn clean test -DsuiteXmlFile=...`)
  -> docker cp target/surefire-reports out of the container
  -> docker rm
  -> exit with the container's exit code           <- determines CodeBuild PASS/FAIL
```

No `|| true`, and `maven.test.failure.ignore` is not set anywhere - a single
failing test fails the Maven build, which fails the container's exit code, which
fails the CodeBuild phase.

## 11. IAM permissions required (CodeBuild service role)

Minimum permissions only - no `AdministratorAccess`, no `Resource: "*"` except
where AWS requires it (called out below). Replace placeholders with your actual
account ID/region/parameter path.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "EcrAuth",
      "Effect": "Allow",
      "Action": "ecr:GetAuthorizationToken",
      "Resource": "*"
    },
    {
      "Sid": "EcrPullPush",
      "Effect": "Allow",
      "Action": [
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload"
      ],
      "Resource": "arn:aws:ecr:<AWS_REGION>:<AWS_ACCOUNT_ID>:repository/brandrunner-api-automation"
    },
    {
      "Sid": "SsmAutomationCredentials",
      "Effect": "Allow",
      "Action": [
        "ssm:GetParameter",
        "ssm:GetParameters"
      ],
      "Resource": "arn:aws:ssm:<AWS_REGION>:<AWS_ACCOUNT_ID>:parameter/brandrunner/qa/automation/*"
    },
    {
      "Sid": "KmsDecryptSsmSecureString",
      "Effect": "Allow",
      "Action": "kms:Decrypt",
      "Resource": "arn:aws:kms:<AWS_REGION>:<AWS_ACCOUNT_ID>:key/<KMS_KEY_ID>"
    },
    {
      "Sid": "CloudWatchLogs",
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents"
      ],
      "Resource": "arn:aws:logs:<AWS_REGION>:<AWS_ACCOUNT_ID>:log-group:/aws/codebuild/<CODEBUILD_PROJECT_NAME>:*"
    },
    {
      "Sid": "CodeBuildTestReports",
      "Effect": "Allow",
      "Action": [
        "codebuild:CreateReportGroup",
        "codebuild:CreateReport",
        "codebuild:UpdateReport",
        "codebuild:BatchPutTestCases"
      ],
      "Resource": "arn:aws:codebuild:<AWS_REGION>:<AWS_ACCOUNT_ID>:report-group/<CODEBUILD_PROJECT_NAME>-*"
    }
  ]
}
```

Notes:

- `ecr:GetAuthorizationToken` is the one action here that AWS does not support
  resource-level restriction on at all - `Resource: "*"` is required by the AWS
  IAM/ECR permission model, not a shortcut taken here.
- The `KmsDecryptSsmSecureString` statement is only needed if the SSM
  SecureString parameters use a **customer-managed** KMS key. If you use the
  AWS-managed `aws/ssm` key (the default when creating a SecureString parameter
  without specifying a KMS key), the calling principal still needs `kms:Decrypt`
  granted by *some* policy attached to it - either keep this statement pointed
  at the `aws/ssm` key's ARN, or omit it if your account's default SSM
  encryption setup already grants this some other way. Confirm which applies
  once the parameters are actually created.
- The `SsmAutomationCredentials` statement uses the wildcard
  `parameter/brandrunner/qa/automation/*`, so it **already covers the UI parameters** used by `buildspec-ui.yml`
  (`.../automation/onboarding-ui-username` and `.../automation/onboarding-ui-password`); no new SSM statement is
  needed. The same KMS rule applies to the UI password, which is a SecureString: if it is encrypted with a
  **customer-managed** KMS key, the CodeBuild service role needs `kms:Decrypt` on that key (the
  `KmsDecryptSsmSecureString` statement), and the key policy must allow the role. If it uses the default AWS-managed
  `aws/ssm` key, follow the note above.
- If your CodeBuild project also uploads `artifacts:` to an S3 bucket (the
  `artifacts:` block in both buildspec files, if you wire that up to an S3
  location rather than leaving it as build-only output), add a scoped
  `s3:PutObject`/`s3:GetObject` statement for that specific bucket/prefix - not
  included above since it depends on your project's artifact configuration.

## 12. SSM Parameter Store variables

`buildspec.yml`/`buildspec-test-only.yml` both declare:

```yaml
env:
  parameter-store:
    API_USERNAME: "/brandrunner/qa/automation/username"
    API_PASSWORD: "/brandrunner/qa/automation/password"
```

CodeBuild resolves these SecureString parameters and injects them as
`API_USERNAME`/`API_PASSWORD` container-build-phase environment variables
automatically - no application code calls the SSM API directly for this path
(unlike the Canary's `ParameterStoreCredentialResolver`, which the CodeBuild
path does not use or depend on).

**On naming**: the Canary already uses `/brandrunner/qa/canary/username` and
`/brandrunner/qa/canary/password`. Do not rename or delete those - the Canary
still depends on them. For CodeBuild, this document uses new parameter names:

```
/brandrunner/qa/automation/username
/brandrunner/qa/automation/password
```

**Recommendation**: create these as new parameters (even if the value is
identical to the Canary's today) rather than pointing CodeBuild at the
`.../canary/...` parameters. `canary` in the name would be actively misleading
once CodeBuild is the primary/only execution path and the Canary is eventually
retired - and keeping them separate means either one's rotation/retirement
never risks breaking the other. Create with, for example:

```bash
aws ssm put-parameter \
  --name "/brandrunner/qa/automation/username" \
  --type "String" \
  --value "<qa-username>" \
  --region <AWS_REGION>

aws ssm put-parameter \
  --name "/brandrunner/qa/automation/password" \
  --type "SecureString" \
  --value "<qa-password>" \
  --region <AWS_REGION>
```

(Not run automatically by this change - AWS resource creation is left to you,
per the instruction this document was written under.)

### Parameters for UI and combined runs (`buildspec-ui.yml`)

`buildspec-ui.yml` additionally declares two parameters for the Onboarding Portal account:

```yaml
env:
  parameter-store:
    API_USERNAME: "/brandrunner/qa/automation/username"
    API_PASSWORD: "/brandrunner/qa/automation/password"
    ONBOARDING_UI_USERNAME: "/brandrunner/qa/automation/onboarding-ui-username"
    ONBOARDING_UI_PASSWORD: "/brandrunner/qa/automation/onboarding-ui-password"
```

CodeBuild **fails the build if any listed parameter does not exist**, so create both UI parameters before the first
run. This applies even when `TEST_TARGET=ui` (the two `API_*` parameters are listed too). Create them with the
placeholders below, substituting your own values. The password is read from a hidden prompt so it is never typed on
a command line or kept in shell history:

```bash
aws ssm put-parameter \
  --name "/brandrunner/qa/automation/onboarding-ui-username" \
  --type "String" \
  --value "<onboarding-portal-username>" \
  --region <AWS_REGION>

read -rsp "Onboarding portal password: " ONBOARDING_PW; echo
aws ssm put-parameter \
  --name "/brandrunner/qa/automation/onboarding-ui-password" \
  --type "SecureString" \
  --value "$ONBOARDING_PW" \
  --region <AWS_REGION>
unset ONBOARDING_PW
```

(Add `--key-id <KMS_KEY_ID>` to encrypt with a customer-managed key instead of the default `aws/ssm` key, and see
section 11 for the matching `kms:Decrypt` permission. As above, these commands are not run by this change.)
Use a dedicated QA account for automation; the UI suite only reads, but treat the password as a real secret.

## 13. Buildspec configuration

Already created in this repo:

- **`buildspec.yml`** - build image, push to ECR (immutable tag + `latest`), run
  the API suite, extract reports. Use this for the first CodeBuild project.
- **`buildspec-test-only.yml`** - pull an existing ECR image, run the API suite,
  extract reports. Use this for a second CodeBuild project only if/when you
  adopt the two-stage split from section 9.

## 14. Running the build manually

Console: open the CodeBuild project > "Start build" (optionally override
`IMAGE_TAG`/other environment variables for that one run under "Environment
variables override").

CLI:

```bash
aws codebuild start-build --project-name <your-codebuild-project-name> --region <AWS_REGION>
```

## 15. Viewing CodeBuild logs

Console: the CodeBuild project's build run page shows phase-by-phase output live
and after completion (Build details > Phase details, or the "Tail logs"/"View
entire log" links).

## 16. Viewing CloudWatch Logs

CodeBuild sends build output to CloudWatch Logs automatically as long as the
project has CloudWatch Logs enabled (default) and the service role has the
`CloudWatchLogs` permissions from section 11. Console: CloudWatch > Log groups >
`/aws/codebuild/<your-project-name>` > the log stream named after the build ID.
This includes all Maven/TestNG/API output, including `SanitizedApiLoggingFilter`'s
sanitized request/response logging - credentials, tokens, and cookies remain
masked exactly as they are in local runs (see section "Sanitized logging" below).

## 17. Viewing TestNG reports

- **CodeBuild's own "Reports" tab** (Console: CodeBuild project > Build run >
  Reports tab) - populated from the `reports:` block in `buildspec.yml`, which
  points at `target/surefire-reports/testng-results.xml` using CodeBuild's
  native `TESTNGXML` report format. This is the file TestNG's Surefire
  integration actually produces - verified directly against a real local run of
  this project (see "Validation performed" in the final report below), not
  assumed.
- **Raw report files** - `target/surefire-reports/**/*` is also published as a
  CodeBuild build `artifacts:` output, so the full Surefire/TestNG report
  directory (including `TEST-TestSuite.xml`, the JUnit-format equivalent, and
  the HTML reports) is downloadable from the build's Artifacts tab if you
  configure an artifacts bucket for the project.

## 18. Troubleshooting

| Symptom | Likely cause |
|---|---|
| `docker: command not found` / docker phase fails immediately | Privileged mode not enabled on the CodeBuild project (section 7) |
| `docker login` fails with 403 | Service role missing `ecr:GetAuthorizationToken` (section 11) |
| `docker push` fails with 403 | Service role missing the `EcrPullPush` actions, or wrong repository ARN in the policy |
| Container fails immediately with a `CanaryConfig`/`ConfigManager` "No API username configured" error | `API_USERNAME`/`API_PASSWORD` not resolved - check the `parameter-store` block's parameter names actually exist in SSM, and the service role has `ssm:GetParameter(s)` + (if needed) `kms:Decrypt` |
| Reports tab is empty | `docker cp` step didn't find `target/surefire-reports` - check the `post_build` logs for the actual `mvn` output; a very early failure (e.g. compile error) may mean Surefire never ran at all |
| Build always "succeeds" even when a test fails | Something reintroduced `maven.test.failure.ignore=true` or a `|| true` - grep `pom.xml` and both buildspec files; neither should contain either |
| Container's `mvn test` fails trying to download a dependency | `dependency:go-offline` in the Dockerfile's `deps` stage doesn't guarantee every plugin/transitive is pre-cached; the container falls back to live Maven Central access at run time. If the CodeBuild project runs inside a VPC with restricted internet egress, ensure a route to Maven Central (or an internal mirror) exists, or the run will fail on a cold cache |
| Container exits **3** right away with "needs a non-blank ONBOARDING_UI_USERNAME and/or ONBOARDING_UI_PASSWORD" | `TEST_TARGET` is `ui` or `all` and one of the two UI credentials is unset, empty or whitespace-only. Check that the `onboarding-ui-username` / `onboarding-ui-password` SSM parameters exist, are not empty, and that `buildspec-ui.yml` passes them (section 12). No test was started. The message names the variable, never its value |
| Container exits **4**, log shows `=== <api\|ui> suite FAILED: Maven succeeded but the TestNG report check failed` | Maven returned 0 but the report line above it shows why the check failed: `report not found`/`invalid`/`inconsistent` (Surefire did not produce a usable `testng-results.xml`), `the report contains failures` (a failed test or configuration method that Maven was told to ignore), or `no test was executed` (zero tests, everything skipped, or a `UI_GROUPS`/`-Dgroups` value that matches no test). Fix the cause; do not remove the check |
| Image build fails downloading Chrome or at `sha256sum -c`, or `apt-get` fails | No outbound access to `storage.googleapis.com` or the Ubuntu mirrors (see the network prerequisites in section 21), or a `CHROME_VERSION` override without its matching `CHROME_SHA256` / `CHROMEDRIVER_SHA256` |

## 19. Sanitized logging

`SanitizedApiLoggingFilter` (unchanged by this work) continues to mask
`Authorization`/`Cookie`/`Set-Cookie`/`cron-secret` headers and sensitive JSON
response fields (keys/tokens/secrets/passwords, AWS access key IDs, presigned
URL credentials) in all Docker/CodeBuild output exactly as it already does for
local runs - this is REST Assured-level logging, unaffected by where the JVM
happens to be running.

## 20. Scheduling execution later

Not configured by this change (per "do not deploy/create AWS resources
automatically"). When ready:

- **EventBridge Scheduler / CloudWatch Events rule** targeting
  `codebuild:StartBuild` on a cron/rate expression - the standard way to run a
  CodeBuild project on a schedule without any code changes here.
- If you adopt the two-stage split (section 9), schedule the **test execution**
  project (`buildspec-test-only.yml`), not the image-build one - that keeps
  scheduled runs fast (pull + run only) while image builds stay tied to source
  changes.

---

## 21. UI automation in the same image (headless Chrome)

The image also contains a pinned **Chrome for Testing** browser and its matching ChromeDriver, so the Onboarding
Portal UI suite (`src/test/resources/testng/testng-onboarding-ui.xml`) can run in the same container. API-only
stays the default.

### Choosing what runs: `TEST_TARGET`

| `TEST_TARGET` | Runs | Notes |
|---|---|---|
| `api` (default, also when unset) | `testng-api.xml` | Original behaviour. No browser is started. |
| `ui` | `testng-onboarding-ui.xml` | Needs non-blank `ONBOARDING_UI_USERNAME` and `ONBOARDING_UI_PASSWORD`. |
| `all` | API, then UI | Both always run, even if the first fails; the container exits non-zero if **either** fails. |

`UI_GROUPS` (optional) narrows the UI run to existing TestNG group(s), e.g. `smoke` or `regression`; empty means the
whole UI suite. The groups themselves are unchanged. The container's entrypoint is `docker/run-tests.sh`, which runs
the project's own `mvn clean test -DsuiteXmlFile=...` command.

**Exit codes.** `0`: every selected suite passed. Otherwise the exit code of the **first failing suite in run order**
(API, then UI): Maven's own exit code when Maven failed, or `4` when Maven succeeded but the report check below
failed. Before any test runs: `2` = unknown `TEST_TARGET`, `3` = missing UI credentials.

**Credential check.** For `ui` and `all`, an unset, empty or whitespace-only `ONBOARDING_UI_USERNAME` or
`ONBOARDING_UI_PASSWORD` is treated as missing: the container prints which variable name is missing (never a value)
and exits 3 before starting Maven.

**Report guard.** After each suite, `docker/CheckReport.java` (a single-file Java program using the JDK's XML
parser, so nothing extra is installed) reads `target/surefire-reports/testng-results.xml`. A suite fails, with exit 4
if Maven itself succeeded, when:

- the report is missing, not valid XML, not a TestNG results file, or its counts are inconsistent. TestNG's root
  attributes satisfy `total = passed + failed + skipped + ignored` (checked against real reports of this project);
- the report contains **any failure**: `failed` is above 0, or any `test-method` has status FAIL, **including a failed
  configuration method** (for example a failing `@BeforeClass` login), which TestNG does not include in `failed`. This
  means a forwarded `-Dmaven.test.failure.ignore=true` cannot produce a green build;
- `total` is 0 or **no test executed**. Executed means passed + failed: skipped and ignored (group-filtered or
  `enabled = false`) tests do not count, so a typo in `UI_GROUPS` or a run in which everything was skipped cannot be a
  green build.

The counts (`total`, `executed`, `passed`, `failed`, `skipped`, `ignored`, failed configuration methods) are printed
after every suite, so skipped tests are always visible. If Maven itself fails, Maven's exit code is kept. The checker
is fail-closed: if it cannot run, or its XML security settings (no DOCTYPE, no external DTD or schema access, no
XInclude, no entity expansion) cannot be applied, the suite fails instead of passing.

**Extra arguments.** Arguments after the image name are appended to every Maven command, as they were when the
entrypoint was the bare `mvn ... test` command, e.g. `docker run <image> -X`. Arguments come last, so a repeated
`-D` option overrides the one the entrypoint set. With `TEST_TARGET=all` the same arguments go to **both** suites, so
use them only for options valid for both; for example `-Dgroups=smoke` would also filter the API suite, which has no
such group, and the zero-test guard would fail it. Use `UI_GROUPS` to narrow only the UI suite.

### Environment variables (names already used by the framework; none are baked into the image)

- API: `API_BASE_URL`, `API_TENANT`, `API_USERNAME`, `API_PASSWORD` (unchanged).
- UI: `ONBOARDING_UI_USERNAME`, `ONBOARDING_UI_PASSWORD` (required), `ONBOARDING_UI_URL` (optional; defaults to
  `onboarding.ui.url` in `config.properties`), `ONBOARDING_UI_HEADLESS` / `UI_HEADLESS` (optional; the framework runs
  Chrome headless unless told otherwise).

### Examples

```bash
docker build -t brandrunner-automation:local .

# API only (default, unchanged)
docker run --rm -e API_BASE_URL=... -e API_TENANT=... -e API_USERNAME=... -e API_PASSWORD=... brandrunner-automation:local

# UI only (read-only suite; narrow with UI_GROUPS if wanted)
docker run --rm -e TEST_TARGET=ui -e UI_GROUPS=smoke \
  -e ONBOARDING_UI_USERNAME=... -e ONBOARDING_UI_PASSWORD=... brandrunner-automation:local

# Both
docker run --rm -e TEST_TARGET=all -e API_BASE_URL=... -e API_TENANT=... -e API_USERNAME=... -e API_PASSWORD=... \
  -e ONBOARDING_UI_USERNAME=... -e ONBOARDING_UI_PASSWORD=... brandrunner-automation:local
```

### Browser and driver

- Chrome for Testing `155.0.8059.39` and ChromeDriver `155.0.8059.39` (a matched pair), downloaded from Google's
  Chrome for Testing storage during the image build and **verified against pinned SHA-256 values**. Upgrade by
  changing `CHROME_VERSION`, `CHROME_SHA256` and `CHROMEDRIVER_SHA256` together in the `Dockerfile`.
- **Platform: x86_64 / AMD64 only.** Chrome for Testing is published for linux64 (x86_64) only, so the image build
  refuses to continue on any other architecture, and the CodeBuild environment must be x86_64 (section 7). On an Apple
  Silicon (arm64) machine, build and run the image for the target platform explicitly (it runs under emulation, so
  expect it to be slower):

  ```bash
  docker build --platform linux/amd64 -t brandrunner-automation:local .
  docker run --rm --platform linux/amd64 ... brandrunner-automation:local
  ```

  This restriction and the browser pin are unchanged; they would only be revisited if an ARM environment were actually
  needed.
- A missing Chrome shared library fails the image build (a dedicated `ldd` check in the `Dockerfile`).
- The framework keeps using WebDriverManager (`BaseDriver` is unchanged). Its cache (`WDM_CACHEPATH`) is pre-seeded
  with the verified driver and `WDM_CHROMEDRIVERVERSION` is pinned, so no driver is downloaded at run time.
- `BaseDriver` already passes `--headless`, `--no-sandbox`, `--disable-dev-shm-usage` and `--remote-allow-origins=*`;
  nothing was added. About `--no-sandbox`: Chrome's own sandbox relies on kernel features such as user namespaces and
  specific system calls, and the default security restrictions of a container runtime (including the nested Docker
  that CodeBuild runs) commonly block them, so the sandbox cannot start normally. Disabling it makes Chrome work in
  the container, but it is a **security trade-off**: a compromised renderer would lose that layer of isolation. It is
  acceptable here because the container is disposable, runs as a non-root user and only visits the portal under test;
  it is not a reason to point this browser at untrusted sites. Running as non-root is a separate, additional
  protection, not the reason for the flag. `--disable-dev-shm-usage` is for the small default `/dev/shm` of containers.
- The container still runs as the non-root `apprunner` user.

### Network prerequisites (outbound)

- **Docker image build** (`docker build`, for example in `buildspec.yml`): the Ubuntu package mirrors that
  `apt-get` uses (to install Chrome's runtime libraries and `curl`/`unzip`), the Chrome for Testing download host
  `storage.googleapis.com` (the pinned Chrome and ChromeDriver archives, verified against SHA-256), plus what the build
  already needed: Maven Central for dependencies and the base-image registry.
- **Test execution** (`docker run` / `buildspec-ui.yml`): Maven's dependency and plugin repositories as needed (Maven
  Central, unless an internal mirror is configured; the image does not pre-cache every plugin, see section 18), and the
  systems under test (the portal URL, the API base URL). ChromeDriver is **not** downloaded at run time: it is seeded
  in the image.
- In a VPC with restricted egress, allow these hosts (or provide mirrors) for the stage that needs them. These are
  documented requirements and have not been verified from inside a CodeBuild VPC.

### CodeBuild

`buildspec.yml` and `buildspec-test-only.yml` **build the image / pull it and then run it with `docker create` and
`docker start -a`** inside CodeBuild, which is why privileged mode is required (section 7). The image's own
entrypoint is used, which is now `docker/run-tests.sh`; with `TEST_TARGET` unset those two buildspecs behave exactly as
before (API only). The image build is larger and slower than before (Chrome and its libraries add roughly 0.7 GB; measured: 260 MB of OS libraries plus 466 MB of browser and driver).

For UI or combined runs use **`buildspec-ui.yml`** on a second CodeBuild project (test-only: it pulls an image built by
`buildspec.yml`). It adds `TEST_TARGET`, `UI_GROUPS` and the `ONBOARDING_UI_*` variables, copies per-suite reports from
`/app/test-reports/{api,ui}/` into `test-reports/{api,ui}/`. Its two `ONBOARDING_UI_*` SSM parameters (section 12) must
exist before the first run, because CodeBuild fails a build when a listed parameter does not exist.

Report declaration: it uses **one** report group with the glob `**/testng-results.xml` under `test-reports/`, instead
of one group per suite, so the declaration never names a file that may not exist. `TEST_TARGET=ui` produces only
`ui/testng-results.xml` and matches only that; `TEST_TARGET=all` produces both and matches both (CodeBuild merges
them into one report), and the raw per-suite folders are also kept as build artifacts. **This report-group behaviour
has not been verified in AWS CodeBuild** (only the container, locally in Docker, was run). On the first real build,
check the build's phase details and Reports tab; if CodeBuild treats the report declaration differently than
expected, switch to one buildspec per target.

If a CodeBuild project instead used this image directly as its build environment, do
not rely on the Docker `ENTRYPOINT`; call `docker/run-tests.sh` from the buildspec `commands` (CodeBuild overrides
image entrypoints).

### Reports

A single-suite run keeps the existing location, `/app/target/surefire-reports`. Every run also copies each suite's
reports to `/app/test-reports/<api|ui>/`, which survives the `mvn clean` of the next suite in an `all` run (in an `all`
run `/app/target/surefire-reports` itself holds only the last suite, UI).

Before copying, only **that suite's** destination directory is removed and recreated, so a reused directory (for
example a mounted volume) never keeps stale files from an earlier run, and the other suite's directory is left alone.
A copy problem prints a `WARNING` on stderr and never changes the suite's result; in `buildspec-ui.yml` a failed
`docker cp` of the reports likewise prints a `WARNING` and keeps the container's real exit code.

### Safety

The UI suite is read-only: no test clicks Create or Update, changes a setting or status, uploads a file, or adds or
removes a cron. The Critical E2E tests E2E-002 and E2E-003 (writes) remain skipped by scope decision and do not exist
in the suite. Credentials are supplied only at run time, `.dockerignore` still excludes `credentials.properties` and
`input.properties`, and the framework's secret masking is unchanged.

---

## Why not a JRE-only final stage

A true minimal-runtime Docker image would need a self-contained runnable
artifact (a shaded/fat jar bundling TestNG + all dependencies, plus a small
custom `main()` that invokes TestNG programmatically against
`testng-api.xml`) instead of shelling out to `mvn test`. Building that runner
would be new execution machinery this project doesn't have today - out of scope
for "make only the changes required for Docker + CodeBuild execution" and
"do not create a new automation framework." The two-stage Dockerfile here keeps
Maven+JDK in the final image specifically so it can keep running the exact same,
already-documented `mvn clean test -DsuiteXmlFile=...` command unchanged.
