# 008 Classification And Semantic Events

## Goal

Use gpt-5.4-mini to classify news items and assign them to semantic events.

## Scope

- Build classification requests with news item, category options, publishable categories, free-form editorial rules, and candidate semantic events.
- Limit candidate semantic events by configured lookup window.
- Persist classifier decisions.
- Persist the editorial rules version used for each classifier decision.
- Create or match `semantic_news_events`.
- Link `news_items` to semantic events.

## Deliverables

- `CategorizationService`.
- `SemanticEventGroupingService`.
- `OpenAiCategorizer`.
- `ClassificationRun` persistence.
- Semantic event repository operations.

## Tests

- Service tests using mocked OpenAI categorizer.
- Tests for `matched_existing` and `created_new`.
- Tests for invalid category codes and invalid semantic key actions.
- Tests for editorial-rule exclusion and `EDITORIAL_RULE_EXCLUDED`.
- Fixture tests for classification JSON.

## Done When

- News items are linked to semantic events.
- Classification runs are persisted for audit.
- Editorial rules can influence `shouldPublish` without code changes.
- Cross-source duplicate handling uses semantic keys, not fingerprints.
