# 013 Cleanup Service

## Goal

Clean up old operational records according to configured retention periods.

## Scope

- Implement cleanup scheduler and service.
- Use configurable retention period.
- Delete or archive old operational records according to chosen policy.
- Do not delete enabled configuration records.

## Deliverables

- `CleanupScheduler`.
- `CleanupService`.
- Cleanup repository methods.
- Cleanup configuration.

## Tests

- Unit tests for cutoff calculation.
- Service tests with mocked repositories.
- Tests ensuring configuration records are not deleted.

## Done When

- Cleanup runs on schedule.
- Old operational records are removed or archived.
- Cleanup failures do not stop ingestion or publication.
