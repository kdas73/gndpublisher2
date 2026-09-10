# Agent Instructions

## Project

GND Publisher is a Spring Boot application on Java 21. The application periodically reads RSS feeds from selected Greek news websites, stores fetched news in a database, filters them by configured categories, translates selected items into target languages, and publishes them to language-specific Telegram channels with a clear source attribution.

## Core Stack

- Java: 21
- Framework: Spring Boot
- Build tool: Gradle
- Containerization: Docker
- Cloud runtime: Kubernetes service on AWS
- Local database: SQLite 3
- Cloud database: PostgreSQL
- Database migrations: Liquibase
- Scheduler: Spring scheduling unless a stronger project requirement appears
- LLM providers: pluggable per use case (OpenAI Responses API, local Ollama), selected by configuration
- Categorizer: the LLM configured for classification
- Translator and summary generator: the LLM configured for publication content
- External integrations: RSS feeds, LLM provider APIs, Telegram Bot API

## Important Docs

- Product overview: `docs/overview.md`
- Architecture: `docs/architecture.md`
- Development and runtime profiles: `docs/development.md`
- Security and secrets: `docs/security.md`
- Database design: `docs/db.md`
- LLM JSON contracts and provider wire formats: `docs/llm.md`

## Working Rules

- Keep changes aligned with Spring Boot conventions and Java 21.
- Prefer explicit configuration by Spring profiles over environment-specific branching in application code.
- Local development must use the SQLite profile.
- Cloud deployments must use the `prod` Spring profile with PostgreSQL.
- Cloud deployments must be packaged as Docker images and deployed to Kubernetes on AWS.
- Database schema changes must be represented as Liquibase changesets.
- Do not commit secrets, tokens, channel IDs, API keys, local database files, or local override config.
- Select the LLM provider per use case through `gnd.llm.<use-case>.provider`; never wire a provider adapter directly into a service.
- Cross-source duplicate detection must use LLM-assisted semantic event grouping with semantic keys, not fingerprints.
- Keep all possible categories and categories selected for publication as separate configuration lists.
- Free-form editorial classification rules must be configurable and passed to the classifier when present.
- Items excluded by editorial rules must use `rejection_reason = EDITORIAL_RULE_EXCLUDED`.
- Limit selected publishable items per RSS source per run using configurable source quota settings.
- Items rejected only by source quota must use `rejection_reason = SOURCE_RUN_QUOTA_EXCEEDED`.
- Important news digest publishing must be scheduled, configurable, idempotent, and link only to already published Telegram posts.
- Use the configured publication content LLM for translation and for news summary generation when the RSS summary is missing or insufficient.
- LLM request and response DTOs must follow `docs/llm.md` and live under `com.gnd.publisher.dto.llm`.
- Keep provider-specific code inside `integration/llm/<provider>`; schema construction, parsing, normalization, and validation stay provider-agnostic.
- Adding a provider means a new adapter package, two `@Bean` methods in `LlmClientsConfiguration`, and one arm in `LlmProperties.readTimeout`.
- Persist the provider id reported by the completion, never a hardcoded provider literal.
- LLM prompt templates must be stored as text files under `src/main/resources/prompts/`, not hardcoded in Java classes.
- Follow the package structure and dependency direction defined in `docs/architecture.md`.
- When iterating over collections or processing item streams, prefer reactive streams where they fit the flow and keep the code readable.
- When checking nullable values, prefer `Optional.ofNullable(...)` for null-safe transformations and branching.
- Cleanup of old records must run through a scheduled cleanup service and use configurable retention periods.
- When touching database entities, repositories, migrations, or query logic, update `docs/db.md` if the schema or data model changes.
- When changing feed ingestion, translation, category routing, or Telegram publishing behavior, update `docs/architecture.md` if the workflow changes.
- Keep source attribution visible in published Telegram messages.
- Unit tests should use JUnit 5, AssertJ, and Mockito.
- Do not start the Spring application context in regular unit tests.
- Do not call real RSS feeds, LLM providers (hosted or local, including Ollama), Telegram, AWS, PostgreSQL, or other external services from unit tests.
- LLM JSON contracts should have fixture-based tests that deserialize request and response examples into DTOs.

## Expected Commands

The exact build commands should be confirmed after the project scaffold is added. Expected defaults:

- Run locally: `./gradlew bootRun --args='--spring.profiles.active=local'`
- Run tests: `./gradlew test`
- Build: `./gradlew build`

On Windows PowerShell, use `.\gradlew.bat` when the Gradle wrapper is present.

## Verification

- Run tests before finalizing changes that affect application behavior.
- Run Liquibase migrations against the active profile when database changes are introduced.
- For Telegram publishing changes, verify that language-to-channel routing and source attribution remain intact.
