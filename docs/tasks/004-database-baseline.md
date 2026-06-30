# 004 Database Baseline

## Goal

Create the initial database schema with Liquibase for SQLite local development and PostgreSQL production.

## Scope

- Add Liquibase changelog structure.
- Add tables described in `docs/db.md`.
- Add initial indexes and uniqueness constraints.
- Add JPA entities or equivalent persistence models.
- Configure SQLite for `local` and PostgreSQL for `prod`.

## Deliverables

- Liquibase master changelog.
- Initial schema changesets.
- Entities for news items, semantic events, categories, classification runs, translations, summaries, Telegram channels, and publications.
- Repositories for core entities.

## Tests

- Schema migration test for SQLite profile.
- PostgreSQL migration test can be deferred to Testcontainers task if needed.
- Repository smoke tests only where useful.

## Done When

- Liquibase initializes the local SQLite database.
- Schema matches `docs/db.md`.
- Any schema change updates `docs/db.md`.
