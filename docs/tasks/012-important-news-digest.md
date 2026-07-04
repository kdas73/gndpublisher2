# 012 Important News Digest

## Goal

Publish a scheduled Telegram digest listing already published semantic events that are considered important because they have more than the configured number of semantic duplicates.

## Scope

- Add important news digest configuration.
- Find published semantic events with linked source item count greater than the configured importance threshold.
- Build one Telegram post with titles only.
- Format each title as a link to the original published Telegram post.
- Persist digest posts and digest items to prevent repeated digest publication.
- Do not call OpenAI for digest generation.

## Deliverables

- `ImportantNewsDigestProperties`.
- `ImportantNewsDigestService`.
- `ImportantNewsDigestScheduler`.
- `ImportantNewsDigestMessageMapper`.
- `ImportantNewsDigestPost` and `ImportantNewsDigestItem` persistence.
- Repository methods to find qualifying semantic events and already digested events.

## Tests

- Unit tests for threshold filtering.
- Unit tests for idempotency: already digested semantic events are skipped.
- Unit tests for Telegram message formatting with linked titles.
- Service tests with mocked repositories and Telegram publisher.

## Done When

- The digest runs on schedule.
- Only already published semantic events can be included.
- Events are included only when source item count is greater than the configured threshold.
- Digest posts contain linked titles and no generated summaries.
- Re-running the scheduler does not repeat digest items.
