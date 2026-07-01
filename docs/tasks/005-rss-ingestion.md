# 005 RSS Ingestion

## Goal

Read configured RSS feeds and normalize entries into internal news items.

## Scope

- Implement RSS client and parser.
- Implement `FeedIngestionService`.
- Implement `FeedIngestionScheduler`.
- Normalize RSS fields into `NewsItem`.
- Handle per-source failures without stopping the whole run.
- Add initial RSS sources for local/manual verification as database seed data or migrations.

## Initial Sources

Use these sources in the initial local configuration or seed data:

```text
Kathimerini: https://feeds.feedburner.com/kathimerini/DJpy
Ta Nea: https://www.tanea.gr/feed/
```

These URLs are for runtime/manual verification. Unit tests should use local RSS fixtures and must not call live feeds.

## Deliverables

- `RssClient`.
- `RssFeedParser`.
- `FeedIngestionService`.
- `FeedIngestionScheduler`.
- DTOs for RSS feed items.
- Initial local seed data for Kathimerini and Ta Nea.

## Tests

- Parser unit tests with RSS fixtures.
- Service tests with mocked RSS client and repository.
- Scheduler test verifying it calls ingestion service.
- No unit test should call the live Kathimerini or Ta Nea feeds.

## Done When

- Enabled RSS sources are read on schedule.
- Feed entries are normalized.
- Failed sources are logged and do not stop other sources.
- Kathimerini and Ta Nea are available as initial enabled or easily enabled RSS sources.
