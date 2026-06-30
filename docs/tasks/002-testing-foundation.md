# 002 Testing Foundation

## Goal

Set up the project testing foundation early so every implementation task can add focused tests as it is built.

## Scope

- Add JUnit 5, AssertJ, and Mockito.
- Add fixture directory structure.
- Add base test conventions.
- Add a simple example unit test.
- Leave OpenAI-specific fixtures to the OpenAI implementation tasks, once DTOs exist.
- Leave Testcontainers setup to the first database integration task that needs PostgreSQL-specific behavior.

## Deliverables

- Test dependencies.
- Base test source layout.
- Fixture directory structure.
- Example unit tests for mapper or utility.

## Tests

- Run `./gradlew test`.
- Verify the example unit test passes.

## Done When

- Unit testing stack is ready.
- Future implementation tasks can add unit tests without changing the test foundation.
- Tests do not call live external services.
