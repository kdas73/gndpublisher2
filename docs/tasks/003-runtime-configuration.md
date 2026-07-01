# 003 Runtime Configuration

## Goal

Define typed configuration for profiles, categories, editorial rules, publishing limits, OpenAI, Telegram, scheduler, and cleanup. RSS source links are database records, not runtime properties.

## Scope

- Add `@ConfigurationProperties` classes.
- Add local and prod profile configuration structure.
- Define all possible categories separately from publishable categories.
- Define free-form editorial classification rules and their version.
- Define default and per-source publication limits.
- Define local secret import strategy.

## Deliverables

- `CategoryProperties`.
- `PublishingProperties`.
- `OpenAiProperties`.
- `TelegramProperties`.
- `SchedulerProperties`.
- `CleanupProperties`.
- Documented example config values without secrets.

## Tests

- Unit tests for property binding where practical.
- Validation tests for missing required properties.

## Done When

- Configuration is strongly typed.
- Secrets are not committed.
- Full category list and publishable category list are separate.
- Editorial classification rules and version are configurable.
- Source quota settings are configurable.
