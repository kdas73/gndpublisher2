# Database

The database stores RSS sources, normalized news items, semantic news events, categorization data, translations, classifier audit data, and Telegram publication history.

## Database Engines

- Local development: SQLite 3.
- Cloud deployment: PostgreSQL.

The application must use Spring profiles to select the database connection.

## Migration Tool

Liquibase is responsible for database initialization and updates.

Guidelines:

- Every schema change must be added as a Liquibase changeset.
- Every schema change must be reflected in this `docs/db.md` document in the same change.
- Changesets should be reviewed for compatibility with both SQLite 3 and PostgreSQL.
- Database-specific SQL should be isolated and documented.
- Avoid relying on database behavior that differs silently between SQLite and PostgreSQL.

## Initial Logical Model

### rss_sources

Stores configured RSS feeds.

Expected fields:

- `id`
- `name`
- `url`
- `language`
- `enabled`
- `created_at`
- `updated_at`

`url` is unique so baseline seed data and future source management cannot create duplicate source records for the same feed.

Baseline seed sources:

- Kathimerini: `https://feeds.feedburner.com/kathimerini/DJpy`
- Ta Nea: `https://www.tanea.gr/feed/`

Both baseline sources are initialized with `language = el` and `enabled = true`.

### news_items

Stores normalized feed entries.

Expected fields:

- `id`
- `source_id`
- `source_url`
- `external_id`
- `title`
- `summary`
- `published_at`
- `fetched_at`
- `original_language`
- `semantic_event_id`
- `classification_status`
- `classification_confidence`
- `classified_at`
- `processing_run_id`
- `publication_candidate`
- `selected_for_publication`
- `rejection_reason`
- `created_at`
- `updated_at`

Source-level deduplication should use a stable source identifier where available, with normalized URL fallback. Cross-source deduplication must use semantic event grouping, not fingerprints.

`processing_run_id` is a lightweight application-generated identifier for the current processing run. It is not a foreign key in the MVP.

Publication selection fields:

- `publication_candidate`: true when the item is eligible after classification and category filtering.
- `selected_for_publication`: true when the item survives per-source run quota selection.
- `rejection_reason`: explains why an item did not continue to summary, translation, and publication.

Recommended `rejection_reason` values:

- `NOT_PUBLISHABLE_CATEGORY`
- `EDITORIAL_RULE_EXCLUDED`
- `SOURCE_RUN_QUOTA_EXCEEDED`
- `DUPLICATE_SEMANTIC_EVENT`
- `LOW_CONFIDENCE`
- `CLASSIFICATION_FAILED`

### semantic_news_events

Stores LLM-created or LLM-selected semantic event keys. A semantic event represents one real-world news event that can be reported by multiple RSS sources.

Expected fields:

- `id`
- `semantic_key`
- `category_id`
- `first_seen_at`
- `last_seen_at`
- `status`
- `created_at`
- `updated_at`

`semantic_key` is a short normalized phrase returned by OpenAI GPT5.5-mini. It should not be globally unique across all time because similar events can happen again later. Matching must be controlled by application logic using a configured lookup time window.

### categories

Stores project-defined categories. The application configuration must define all possible categories and a separate list of categories selected for publication.

Expected fields:

- `id`
- `code`
- `name`
- `description`
- `enabled`
- `publishable`
- `publication_priority`

### news_item_categories

Maps news items to categories. Categorization is performed by OpenAI GPT5.5-mini.

Expected fields:

- `news_item_id`
- `category_id`
- `matched_by`
- `model`
- `confidence`
- `created_at`

### classification_runs

Stores classifier decisions for semantic event grouping and category assignment.

Expected fields:

- `id`
- `news_item_id`
- `semantic_event_id`
- `model`
- `input_keys_count`
- `lookup_window_started_at`
- `lookup_window_ended_at`
- `editorial_rules_version`
- `returned_semantic_key`
- `semantic_key_action`
- `matched_semantic_event_id`
- `category_id`
- `confidence`
- `raw_response`
- `created_at`

`raw_response` may contain generated text and should be handled as sensitive operational data. It should be useful for audit and troubleshooting but must not be logged casually.

### translations

Stores translated text for selected semantic events or their canonical news items. Translation is performed by OpenAI GPT-5.5.

Expected fields:

- `id`
- `semantic_event_id`
- `news_item_id`
- `target_language`
- `title`
- `summary`
- `provider`
- `model`
- `created_at`
- `updated_at`

### news_summaries

Stores generated or improved summaries when the RSS item does not provide a suitable summary. Summary generation is performed by OpenAI GPT-5.5.

Expected fields:

- `id`
- `news_item_id`
- `language`
- `summary`
- `provider`
- `model`
- `created_at`
- `updated_at`

### telegram_channels

Stores destination channels.

Expected fields:

- `id`
- `language`
- `channel_id`
- `username`
- `message_url_template`
- `name`
- `enabled`
- `created_at`
- `updated_at`

### publications

Stores Telegram publication history.

Expected fields:

- `id`
- `semantic_event_id`
- `news_item_id`
- `translation_id`
- `telegram_channel_id`
- `target_language`
- `telegram_message_id`
- `telegram_message_url`
- `status`
- `error_message`
- `published_at`
- `created_at`
- `updated_at`

Publication uniqueness should prevent sending the same semantic event to the same Telegram channel and target language more than once.

Recommended uniqueness:

```text
unique(semantic_event_id, telegram_channel_id, target_language)
```

### important_news_digest_posts

Stores Telegram digest posts that list already published semantic events considered important because they have more than the configured number of semantic duplicates.

Expected fields:

- `id`
- `telegram_channel_id`
- `target_language`
- `duplicate_threshold`
- `telegram_message_id`
- `telegram_message_url`
- `status`
- `error_message`
- `published_at`
- `created_at`
- `updated_at`

### important_news_digest_items

Stores semantic events included in an important news digest post.

Expected fields:

- `id`
- `important_news_digest_post_id`
- `semantic_event_id`
- `publication_id`
- `telegram_channel_id`
- `target_language`
- `title`
- `publication_url`
- `source_item_count`
- `created_at`

Recommended uniqueness:

```text
unique(semantic_event_id, telegram_channel_id, target_language)
```

for digest inclusion.

## Recommended Indexes

- `semantic_news_events(semantic_key)`
- `semantic_news_events(last_seen_at)`
- `news_items(semantic_event_id)`
- `news_items(classification_status)`
- `news_items(processing_run_id, source_id)`
- `news_items(source_id, selected_for_publication)`
- `classification_runs(news_item_id)`
- `classification_runs(created_at)`
- `translations(semantic_event_id, target_language)`
- `publications(semantic_event_id, telegram_channel_id, target_language)`
- `publications(telegram_message_url)`
- `important_news_digest_posts(telegram_channel_id, target_language, published_at)`
- `important_news_digest_items(semantic_event_id, telegram_channel_id, target_language)`

## Retention And Cleanup

A scheduled cleanup service removes or archives old operational records according to a configured retention period.

Cleanup should apply to time-based operational data such as:

- old `news_items`;
- old `semantic_news_events`;
- old `classification_runs`;
- related `news_item_categories`;
- related `translations`;
- related `news_summaries`;
- related `publications`.
- related `important_news_digest_posts`;
- related `important_news_digest_items`.

Cleanup must not remove enabled configuration records such as `rss_sources`, `categories`, or `telegram_channels`.

Retention periods must be configurable per environment. Cleanup implementation must preserve referential integrity and should log deletion or archive counts by table or aggregate type.

## Open Decisions

- Whether full article text is stored or only RSS title and summary.
- Whether Telegram channel routing depends only on language or also on category.
- Whether cleanup should hard-delete records or archive them first.
- Whether a full `processing_runs` table is needed later for operational audit, retries, or dashboards.
