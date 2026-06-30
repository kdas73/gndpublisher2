# 001 Project Scaffold

## Goal

Create the initial Spring Boot Java 21 project using Gradle.

## Scope

- Add Gradle project files and wrapper.
- Add the main Spring Boot application class.
- Create the base package `com.gnd.publisher`.
- Add the initial package structure from `docs/architecture.md`.
- Add basic application configuration files.

## Deliverables

- `build.gradle` or `build.gradle.kts`.
- `settings.gradle` or `settings.gradle.kts`.
- Gradle wrapper files.
- `GndPublisherApplication`.
- Empty package directories or placeholder classes where useful.
- `application.yml`, `application-local.yml`, `application-prod.yml`.

## Tests

- Add a minimal application smoke test only if it does not slow down regular unit tests.
- Verify `./gradlew test` runs.
- Verify `./gradlew build` runs.

## Done When

- The project builds with Gradle.
- Java 21 is configured.
- Spring Boot starts with the `local` profile.

