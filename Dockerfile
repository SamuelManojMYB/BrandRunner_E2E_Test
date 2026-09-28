# Runs ONLY the existing "BrandRunners API Suite" (src/test/resources/testng/testng-api.xml)
# via the project's existing, unmodified Maven/TestNG/REST Assured framework - see
# docs/AWS_CODEBUILD.md for the full CodeBuild/ECR setup this image is built for.
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
COPY --from=deps /app/.m2 /app/.m2
COPY pom.xml .
# src/ is copied without config/credentials.properties (excluded via .dockerignore) -
# this image must never contain real credentials. See ConfigManager.getApiUsername()/
# getApiPassword(): those env vars, not the properties file, are how this container
# receives real credentials at `docker run` time.
COPY src ./src

# Compiles everything (UI/mobile/API all share this one Maven module) so the image
# build fails fast on any compile error, and so the eventual `docker run` only needs
# to re-resolve/re-run tests, not recompile from scratch. This does mean Selenium/
# Appium/POI/etc. Java dependencies are present in /app/.m2 purely because the whole
# project is one shared classpath - no browser/Appium/Android OS-level runtime
# packages are installed, and no UI/mobile test ever executes from this image.
RUN mvn -B -ntp -q test-compile

# No fixed --uid: the maven:3.9-eclipse-temurin-21 base image already has a UID
# 1000 user, so a hardcoded --uid 1000 here collides and fails the build - let
# useradd assign the next free UID instead.
RUN useradd --create-home apprunner \
    && chown -R apprunner:apprunner /app
USER apprunner

# Deterministic: always runs exactly the existing, documented API suite command
# (see testng-api.xml's own header comment). Configuration/credentials are supplied
# entirely through environment variables at `docker run` time - see
# docs/AWS_CODEBUILD.md for the full list (API_BASE_URL, API_TENANT, API_USERNAME,
# API_PASSWORD).
ENTRYPOINT ["mvn", "-B", "-ntp", "clean", "test", "-DsuiteXmlFile=src/test/resources/testng/testng-api.xml"]
