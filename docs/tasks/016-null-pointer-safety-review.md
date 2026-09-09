# 016 Null Pointer Safety Review

## Goal

Audit the full codebase for null pointer safety risks and remediate the ones that can realistically occur.

## Scope

- Review service, mapping, and controller layers under `src/main/java` for unchecked dereferences of values that can be null: repository/JPA query results, external API responses (RSS feed parsing, OpenAI responses, Telegram API responses), DTO/entity fields, and configuration properties.
- Prefer `Optional`, `Objects.requireNonNull`, or explicit early-return null checks at boundaries (external API/DB/config) over defensive null checks scattered through business logic.
- Do not introduce blanket null checks where the type/contract already guarantees non-null (e.g. values validated by Bean Validation, non-null constructor params).
- Align with existing error-handling conventions from task 015 (observability/error handling) rather than introducing a new pattern.

## Deliverables

- List of null-safety issues found (by file/method) and the fix applied for each.
- Defensive checks added at genuine risk points (external responses, optional DB results, nullable config).
- Any newly agreed null-safety convention documented briefly (e.g. in `docs/development.md`) if one doesn't already exist.

## Tests

- Unit tests covering the null/empty-response cases that were fixed (e.g. empty RSS feed, missing OpenAI field, null Telegram response) where practical.
- Existing test suite continues to pass.

## Done When

- No unchecked dereferences remain on values sourced from external services, database lookups, or optional configuration.
- Fixes are covered by tests where the null case is realistically reachable.
- Findings and rationale for any accepted risk (not fixed) are documented.
