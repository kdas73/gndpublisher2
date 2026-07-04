# 015 Observability And Error Handling

## Goal

Add baseline observability and consistent error handling across the pipeline.

## Scope

- Add structured logging around run ids, source ids, semantic event ids, and publication ids.
- Add error boundaries for RSS, OpenAI, Telegram, database, and cleanup.
- Add Spring Boot health endpoints suitable for Kubernetes probes.
- Avoid logging secrets or raw OpenAI payloads casually.

## Deliverables

- Logging conventions.
- Error handling conventions.
- Health/readiness/liveness configuration.
- Minimal metrics if available through Spring Boot Actuator.

## Tests

- Unit tests for error mapping where practical.
- Verify health endpoint availability.
- Verify sensitive values are not logged in tested paths.

## Done When

- Failures are diagnosable without exposing secrets.
- External service failures do not crash unrelated pipeline stages.
- Kubernetes probes have suitable endpoints.
