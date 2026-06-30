# Development

This project is planned as a Java 21 Spring Boot application.

## Technology

- Java 21
- Spring Boot
- Gradle
- Docker for cloud packaging
- Kubernetes service on AWS for cloud deployment
- SQLite 3 for local development
- PostgreSQL for cloud runtime
- Liquibase for database migrations

## Runtime Profiles

The application must use Spring profiles for environment-specific configuration.

### Local Profile

Profile name: `local`

Purpose:

- Local development.
- Fast manual testing.
- SQLite 3 database stored in a local file.

Expected run command after Gradle wrapper is added:

```powershell
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

Equivalent Unix command:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### Production Profile

Profile name: `prod`

Purpose:

- Cloud deployment.
- PostgreSQL database.
- Docker image deployed to a Kubernetes service on AWS.
- Production Telegram channels and production credentials.

Use this profile name consistently in code, docs, Docker configuration, and Kubernetes deployment configuration.

## Database

- Local database: SQLite 3.
- Cloud database: PostgreSQL.
- Schema management: Liquibase.

See `docs/db.md` for the database model and migration notes.

## Cloud Deployment

- Build the application with Gradle.
- Package the application as a Docker image.
- Deploy the Docker image to a Kubernetes service on AWS.
- Provide the active profile, database URL, OpenAI settings, Telegram settings, and other runtime configuration through Kubernetes/AWS deployment configuration.
- Do not store cloud secrets in the Docker image or committed manifests.

## Secrets

Secrets must not be committed to the repository.

Local secrets should be stored in a local file excluded by `.gitignore`. See `docs/security.md`.

## Expected Local Setup

1. Install Java 21.
2. Install or use the Gradle wrapper when it exists.
3. Create the local secrets file described in `docs/security.md`.
4. Start the app with the `local` profile.
5. Verify Liquibase applies migrations to the SQLite database.

## Expected Development Commands

These commands should be confirmed after the Spring Boot project is scaffolded:

```powershell
.\gradlew.bat test
.\gradlew.bat build
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

Expected Docker commands should be added after the project Dockerfile is created.

## Testing Strategy

Default test stack:

- JUnit 5
- AssertJ
- Mockito

Unit test rules:

- Keep unit tests fast and deterministic.
- Do not start the Spring application context in regular unit tests.
- Test services with mocked repositories and mocked integration clients.
- Test schedulers minimally: verify that a scheduler calls the expected service method.
- Test mappers, validators, and utility classes as pure unit tests.
- Do not call real RSS feeds, OpenAI, Telegram, AWS, PostgreSQL, or other external services from unit tests.

Integration and contract test rules:

- Test OpenAI JSON contracts with fixture-based tests: request and response examples must deserialize into DTOs.
- Test OpenAI, Telegram, and RSS clients with a mock HTTP server or contract tests, not live external APIs.
- Test Liquibase and database behavior separately from unit tests.
- Use Testcontainers for PostgreSQL integration tests when PostgreSQL-specific behavior matters.
- Use the local SQLite profile for SQLite-specific integration checks.

Recommended test layout after project scaffold:

```text
src/test/java/com/gnd/publisher/
  service/
  scheduler/
  mapper/
  util/
  integration/
  contract/
```

Recommended fixture layout:

```text
src/test/resources/fixtures/openai/
  classification-request.json
  classification-response.json
  summary-request.json
  summary-response.json
  translation-request.json
  translation-response.json
```

## Configuration Guidelines

- Keep RSS source URLs configurable.
- Keep schedule intervals configurable.
- Keep all possible categories configurable.
- Keep categories selected for publication configurable separately from the full category list.
- Keep free-form editorial classification rules configurable.
- Keep an editorial rules version configurable for auditability.
- Keep the default max publishable items per source per run configurable.
- Keep per-source publication limits configurable when a feed needs an override.
- Keep important news digest threshold, schedule, and destination channel configurable.
- Keep target languages configurable.
- Keep Telegram channel mapping configurable.
- Keep OpenAI model names and prompt settings configurable.
- Keep Docker image and Kubernetes deployment configuration environment-specific.
- Keep provider credentials outside committed files.

## Code Organization

- Follow the package structure defined in `docs/architecture.md`.
- Keep scheduled entry points thin and put orchestration in services.
- Keep external API details inside `integration` packages.
- Keep database access inside repositories and schema changes inside Liquibase migrations.
- When changing the database schema, update `docs/db.md` in the same change.
- Add `controller` packages only when REST or admin endpoints are introduced.

## Code Generation Rules

- When iterating over collections or processing item streams, prefer reactive streams where they fit the flow and keep the code readable.
- Avoid blocking inside reactive pipelines unless there is a clear boundary and the blocking call is isolated.
- Do not use reactive streams for trivial local transformations when a simple expression is clearer.
- When checking nullable values, prefer `Optional.ofNullable(...)` for null-safe transformations and branching.
- Do not use `Optional` as a field type in JPA entities or DTOs.
