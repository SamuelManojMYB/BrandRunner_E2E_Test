# Runs the existing "BrandRunners API Suite" (src/test/resources/testng/testng-api.xml) and/or the
# Onboarding Portal UI suite (src/test/resources/testng/testng-onboarding-ui.xml) via the project's existing,
# unmodified Maven/TestNG framework - see docs/AWS_CODEBUILD.md for the full CodeBuild/ECR setup this image is
# built for. Which suite runs is chosen at `docker run` time with TEST_TARGET (api | ui | all); the default,
# api, is exactly the previous behaviour. UI runs use headless Chrome installed in this image.
#
# This is intentionally NOT a JRE-only runtime image: the container's own entrypoint
# invokes `mvn test` directly (matching the exact command already documented in
# testng-api.xml), so a full Maven + JDK toolchain must be present at container run
# time too, not just at image-build time. A true "compile once, ship a thin JRE
# runtime" split isn't practical here without building a shaded/fat jar and a custom
# test runner - both would be a new way of executing this framework, which is out of
# scope (see docs/AWS_CODEBUILD.md's "Why not a JRE-only final stage" note).
#
# Two stages are still used, for Docker layer-caching benefit: the "deps" stage
# resolves this project's Maven dependencies from a pom.xml-only build context, so
# that stage's layer is only invalidated when pom.xml itself changes - not on every
# source edit. The final stage reuses that dependency cache and adds source.
#
# Java version: pom.xml pins maven.compiler.source/target to 21 - this image uses a
# Java 21 Maven base image to match exactly.

FROM maven:3.9-eclipse-temurin-21 AS deps
WORKDIR /app
# Local Maven repo lives under /app/.m2 (not the default ~/.m2) so it can be chowned
# to the non-root user created in the final stage below.
ENV MAVEN_OPTS="-Dmaven.repo.local=/app/.m2/repository"
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

FROM maven:3.9-eclipse-temurin-21
WORKDIR /app
ENV MAVEN_OPTS="-Dmaven.repo.local=/app/.m2/repository"

# ---- Headless Chrome for the UI suite --------------------------------------------------------------------
# Chrome for Testing (a pinned, matched browser + ChromeDriver pair) instead of the floating "Google Chrome
# Stable" apt package: the version is fixed, the pair is guaranteed compatible, and each archive is verified
# against a pinned SHA-256 so a changed or tampered download fails the image build. To upgrade, change all
# three ARGs together (versions: https://googlechromelabs.github.io/chrome-for-testing/ ; hash the two
# linux64 zips with sha256sum). Chrome for Testing publishes linux64 (x86_64) only, so the build refuses to
# continue on another architecture. These layers come before the source copy, so source edits never
# re-download the browser.
ARG CHROME_VERSION=155.0.8059.39
ARG CHROME_SHA256=55672d1f392fd3e7b7a08621b6e804e6bcb39d40cf155504abb74b3a021ea8ea
ARG CHROMEDRIVER_SHA256=a65f025c692e686ec2a21fa6b9639c69fa9813683a47af9cabf1ba0b1f453e42

# Runtime libraries Chrome needs on this Ubuntu (noble) base, plus fonts so pages lay out as usual.
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates curl unzip fonts-liberation \
        libasound2t64 libatk-bridge2.0-0t64 libatk1.0-0t64 libcairo2 libcups2t64 libdbus-1-3 libdrm2 \
        libexpat1 libgbm1 libglib2.0-0t64 libnspr4 libnss3 libpango-1.0-0 libx11-6 libxcb1 \
        libxcomposite1 libxdamage1 libxext6 libxfixes3 libxkbcommon0 libxrandr2 \
    && rm -rf /var/lib/apt/lists/*

RUN set -eux; \
    [ "$(uname -m)" = "x86_64" ] || { echo "Chrome for Testing linux64 needs an x86_64 image build" >&2; exit 1; }; \
    base="https://storage.googleapis.com/chrome-for-testing-public/${CHROME_VERSION}/linux64"; \
    curl -fsSL -o /tmp/chrome.zip "$base/chrome-linux64.zip"; \
    curl -fsSL -o /tmp/chromedriver.zip "$base/chromedriver-linux64.zip"; \
    echo "${CHROME_SHA256}  /tmp/chrome.zip" | sha256sum -c -; \
    echo "${CHROMEDRIVER_SHA256}  /tmp/chromedriver.zip" | sha256sum -c -; \
    unzip -q /tmp/chrome.zip -d /opt; \
    unzip -q /tmp/chromedriver.zip -d /opt; \
    rm -f /tmp/chrome.zip /tmp/chromedriver.zip; \
    ln -s /opt/chrome-linux64/chrome /usr/local/bin/google-chrome; \
    ln -s /opt/chromedriver-linux64/chromedriver /usr/local/bin/chromedriver; \
    mkdir -p "/opt/webdriver-cache/chromedriver/linux64/${CHROME_VERSION}"; \
    cp /opt/chromedriver-linux64/chromedriver "/opt/webdriver-cache/chromedriver/linux64/${CHROME_VERSION}/chromedriver"

# Fails the image build when Chrome has an unresolved shared library. Kept as its own RUN with an explicit
# if/exit (not a trailing `! cmd` in a longer RUN, where a later command could mask the result).
RUN set -eu; \
    missing="$(ldd /opt/chrome-linux64/chrome | grep 'not found' || true)"; \
    if [ -n "$missing" ]; then echo "Chrome is missing shared libraries:" >&2; echo "$missing" >&2; exit 1; fi

# The framework keeps using WebDriverManager (BaseDriver.startApp is unchanged). Its cache is pre-seeded with the
# verified driver above and its driver version is pinned to the browser's, so at run time it finds the matching
# driver in the cache instead of resolving and downloading one. The cache is writable by the non-root user (below).
ENV WDM_CACHEPATH=/opt/webdriver-cache \
    WDM_CHROMEDRIVERVERSION=${CHROME_VERSION}

COPY --from=deps /app/.m2 /app/.m2
COPY pom.xml .
# src/ is copied without config/credentials.properties (excluded via .dockerignore) -
# this image must never contain real credentials. See ConfigManager.getApiUsername()/
# getApiPassword(): those env vars, not the properties file, are how this container
# receives real credentials at `docker run` time.
COPY src ./src
COPY docker/run-tests.sh /usr/local/bin/run-tests.sh
# CheckReport.java is run by run-tests.sh (as a single-file Java program) to verify that a suite really executed tests.
COPY docker/CheckReport.java /usr/local/lib/brandrunner/CheckReport.java
# LF endings and the executable bit regardless of how the file was checked out (e.g. CRLF on Windows).
RUN sed -i 's/\r$//' /usr/local/bin/run-tests.sh && chmod 0755 /usr/local/bin/run-tests.sh

# Compiles everything (UI/mobile/API all share this one Maven module) so the image
# build fails fast on any compile error, and so the eventual `docker run` only needs
# to re-resolve/re-run tests, not recompile from scratch. The mobile (Appium) tests are not
# runnable from this image: no Android/Appium OS-level runtime is installed.
RUN mvn -B -ntp -q test-compile

# No fixed --uid: the maven:3.9-eclipse-temurin-21 base image already has a UID
# 1000 user, so a hardcoded --uid 1000 here collides and fails the build - let
# useradd assign the next free UID instead.
RUN useradd --create-home apprunner \
    && chown -R apprunner:apprunner /app /opt/webdriver-cache
USER apprunner

# TEST_TARGET selects the suite: api (default - the original behaviour: exactly the documented API suite
# command), ui, or all. docker/run-tests.sh runs the project's own `mvn clean test -DsuiteXmlFile=...` command(s),
# appends any arguments given to `docker run` to them, and exits non-zero if any selected suite fails or runs no test. Configuration/credentials are supplied entirely through
# environment variables at `docker run` time - see docs/AWS_CODEBUILD.md for the full list (API_BASE_URL,
# API_TENANT, API_USERNAME, API_PASSWORD, and for UI: ONBOARDING_UI_USERNAME, ONBOARDING_UI_PASSWORD,
# optionally ONBOARDING_UI_URL and UI_GROUPS). No credential is baked into the image.
ENTRYPOINT ["/usr/local/bin/run-tests.sh"]
