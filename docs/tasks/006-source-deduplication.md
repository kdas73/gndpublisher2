# 006 Source Deduplication

## Goal

Prevent storing the same RSS item from the same source multiple times.

## Scope

- Implement URL normalization.
- Implement source-level deduplication.
- Use stable `external_id` when available.
- Use normalized `source_url` fallback.
- Keep cross-source duplicate detection out of this task.

## Deliverables

- `SourceDeduplicationService`.
- `UrlNormalizer`.
- Repository methods for source duplicate lookup.
- `DuplicateSourceNewsItemException` only if useful.

## Tests

- Unit tests for URL normalization.
- Unit tests for external id and URL fallback behavior.
- Service tests with mocked repositories.

## Done When

- Repeated entries from the same RSS source are skipped or updated predictably.
- Cross-source duplicates are still handled later by semantic event grouping.
