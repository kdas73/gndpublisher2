# 009 Source Quota Selection

## Goal

Limit the number of publishable candidates from each RSS source per processing run.

## Scope

- Generate lightweight `processing_run_id`.
- Select candidates after classification and semantic event grouping.
- Apply default max items per source per run.
- Apply per-source overrides when configured.
- Mark rejected items with `SOURCE_RUN_QUOTA_EXCEEDED`.

## Deliverables

- `SourceQuotaSelectionService`.
- `PublishingProperties`.
- Repository methods for selecting and updating candidate status.
- `RejectionReason` enum.

## Tests

- Unit tests for default quota.
- Unit tests for per-source override.
- Tests for sorting by `publication_priority`, `published_at` descending, and confidence descending.
- Tests that quota-rejected items are not sent to summary or translation.

## Done When

- Only selected candidates continue to summary, translation, and publication.
- Rejected items keep a clear `rejection_reason`.
- No `processing_runs` table is required for MVP.
