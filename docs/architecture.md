# Architecture

GND Publisher is a Spring Boot service organized around a scheduled news processing pipeline.

## High-Level Components

- Scheduler: triggers periodic RSS ingestion and publication jobs.
- Feed ingestion: downloads RSS feeds and converts entries into normalized news items.
- Source deduplication: prevents storing the same RSS item from the same source multiple times using source identifiers and normalized URLs.
- Semantic event grouping: uses OpenAI GPT5.5-mini to assign each news item to an existing semantic event key or create a new one.
- Persistence: stores sources, news items, semantic events, categories, translations, classification runs, and publication records.
- Categorization: uses OpenAI GPT5.5-mini to select publication categories and semantic event keys.
- Source quota selection: limits how many publishable items from one RSS source can continue in each run.
- Summary generation: uses OpenAI GPT-5.5 when the RSS item needs a better publication summary.
- Translation: uses OpenAI GPT-5.5 to translate selected news items into target languages.
- Telegram publishing: sends translated messages to Telegram channels.
- Important news digest publishing: periodically publishes a post with links to already published events that are important because they are covered by more than the configured number of semantic duplicates.
- Cleanup: deletes or archives records older than the configured retention period.
- Configuration: defines RSS sources, all possible categories, publishable categories, free-form editorial classification rules, per-run source publication limits, important news digest settings, target languages, Telegram routing, OpenAI settings, and provider credentials.
- Deployment: packages the Spring Boot service as a Docker image and runs it on a Kubernetes service on AWS.

## Processing Pipeline

1. Scheduler starts an ingestion run.
2. The feed ingestion component reads all enabled RSS sources.
3. Each feed entry is normalized into an internal news item.
4. The persistence layer stores only source-level new or changed items.
5. The categorization request includes the current news item, all configured category options, publishable category codes, free-form editorial rules, and existing semantic event keys from a configured lookup time window.
6. OpenAI GPT5.5-mini chooses an existing semantic event key or returns a new one as a short normalized phrase.
7. The news item is linked to the matching or newly created semantic event.
8. Source quota selection keeps only a configured number of publishable candidates per RSS source for the current run.
9. Items not selected by the source quota are marked with `rejection_reason = SOURCE_RUN_QUOTA_EXCEEDED`.
10. OpenAI GPT-5.5 generates or improves a summary when needed for selected candidates.
11. OpenAI GPT-5.5 produces language-specific translated message content for selected publishable semantic events.
12. Telegram publishing sends each translated event to the configured channel for that language.
13. Publication results are stored at semantic event level to prevent duplicate sends and support troubleshooting.

## Important News Digest Pipeline

The important news digest service publishes a periodic list of already published semantic events that are considered important because they have more than the configured number of semantic duplicates.

1. Important news digest scheduler starts on a configured schedule.
2. Important news digest service finds already published semantic events whose linked source item count exceeds the configured importance threshold.
3. Events already included in a previous digest for the same language/channel are skipped.
4. The service builds one Telegram post containing only titles.
5. Each title is formatted as a link to the original published Telegram post for that semantic event.
6. The digest post is sent to the configured Telegram channel.
7. The digest post and its included items are persisted to prevent repeated digest publication.

Rules:

- The importance threshold must be configurable. Default threshold: `2`.
- With threshold `2`, an event qualifies when it has more than two linked source news items.
- Digest selection should use semantic events and publication records, not raw RSS items alone.
- Digest posts must not trigger OpenAI summary or translation.
- Digest posts must be idempotent per target language, Telegram channel, and semantic event.
- Telegram publication records must contain enough data to build links to published posts.

## Source Quota Selection

Each processing run must limit how many publishable news items from one RSS source can continue to summary, translation, and publication.

Rules:

- The default max items per source per run must be configurable.
- Per-source overrides may be configured when some feeds need stricter or looser limits.
- Quota selection runs after source deduplication, semantic event grouping, and categorization.
- Quota selection runs before summary generation, translation, and Telegram publishing.
- Candidates are selected only from items with publishable categories and `shouldPublish = true`.
- Candidates should be sorted by category `publication_priority`, then `published_at` descending, then classification confidence descending.
- Items rejected only because of source quota are not semantically irrelevant; they must be marked with `rejection_reason = SOURCE_RUN_QUOTA_EXCEEDED`.
- Use `processing_run_id` on `news_items` for lightweight traceability. Do not add a separate `processing_runs` table until operational audit, retries, or dashboards require it.

## Semantic Event Grouping

Cross-source duplicate detection is based on LLM-assisted semantic event grouping, not mechanical fingerprints.

For each new news item, the application must send the classifier:

- the news title;
- the news summary or source excerpt;
- the source name and URL;
- the original publication timestamp;
- free-form editorial rules;
- candidate semantic event keys from a configured lookup time window.

The classifier must return structured output:

```json
{
  "categoryCode": "politics",
  "semanticKey": "greek parliament approves new migration bill",
  "semanticKeyAction": "matched_existing",
  "matchedSemanticEventId": "123",
  "confidence": 0.91
}
```

If no existing key fits, `semanticKeyAction` must be `created_new` and `matchedSemanticEventId` must be empty.

Rules:

- Semantic keys are short normalized phrases that describe the event, not hashes.
- Existing keys passed to the classifier must be limited by a configurable time window.
- Semantic keys must not be globally unique across all time because similar events can happen again later.
- Publication idempotency is based on `semantic_event_id`, target language, and Telegram channel.
- Classification decisions should be persisted for audit and troubleshooting.
- Editorial rules are free-form instructions that can exclude or refine publication decisions, for example "for sports news publish only concrete match results" or "ignore celebrity death news".
- Editorial rules should be versioned in configuration so classifier decisions can be audited against the active rule set.

## Cleanup Pipeline

1. Cleanup scheduler starts on a configured schedule.
2. Cleanup service calculates the cutoff timestamp from the configured retention period.
3. Cleanup service removes or archives records older than the cutoff according to database retention rules.
4. Cleanup results are logged with counts per affected table or aggregate type.

Retention periods and cleanup schedules must be configurable per environment. The cleanup process must not delete enabled configuration records such as RSS sources, categories, or Telegram channel definitions.

## Package Structure

Base package:

```text
com.gnd.publisher
```

Recommended package layout:

```text
com.gnd.publisher
  GndPublisherApplication.java

  config
    OpenAiProperties.java
    TelegramProperties.java
    RssSourceProperties.java
    CategoryProperties.java
    EditorialRulesProperties.java
    PublishingProperties.java
    ImportantNewsDigestProperties.java
    SchedulerProperties.java
    CleanupProperties.java
    DatabaseConfig.java

  scheduler
    FeedIngestionScheduler.java
    PublicationScheduler.java
    ImportantNewsDigestScheduler.java
    CleanupScheduler.java

  domain
    model
      NewsItem.java
      SemanticNewsEvent.java
      ClassificationRun.java
      RssSource.java
      Category.java
      Translation.java
      TelegramChannel.java
      Publication.java
      ImportantNewsDigestPost.java
      ImportantNewsDigestItem.java
    enums
      PublicationStatus.java
      ClassificationStatus.java
      SemanticKeyAction.java
      RejectionReason.java
      SourceStatus.java
      Language.java

  dto
    rss
      RssFeedItemDto.java
    openai
      CategoryClassificationRequest.java
      CategoryClassificationResponse.java
      SemanticEventKeyCandidateDto.java
      TranslationRequest.java
      TranslationResponse.java
      SummaryRequest.java
      SummaryResponse.java
    telegram
      TelegramMessageDto.java
    api
      ErrorResponseDto.java

  repository
    NewsItemRepository.java
    SemanticNewsEventRepository.java
    ClassificationRunRepository.java
    RssSourceRepository.java
    CategoryRepository.java
    TranslationRepository.java
    TelegramChannelRepository.java
    PublicationRepository.java
    ImportantNewsDigestPostRepository.java
    ImportantNewsDigestItemRepository.java

  service
    FeedIngestionService.java
    SourceDeduplicationService.java
    SemanticEventGroupingService.java
    CategorizationService.java
    SourceQuotaSelectionService.java
    SummaryService.java
    TranslationService.java
    PublicationService.java
    ImportantNewsDigestService.java
    TelegramRoutingService.java
    CleanupService.java

  integration
    rss
      RssClient.java
      RssFeedParser.java
    openai
      OpenAiClient.java
      OpenAiCategorizer.java
      OpenAiTranslator.java
      OpenAiSummarizer.java
    telegram
      TelegramBotClient.java

  mapper
    NewsItemMapper.java
    SemanticNewsEventMapper.java
    TranslationMapper.java
    TelegramMessageMapper.java
    ImportantNewsDigestMessageMapper.java

  exception
    FeedReadException.java
    OpenAiIntegrationException.java
    TelegramPublishException.java
    DuplicateSourceNewsItemException.java

  util
    UrlNormalizer.java
    TextSanitizer.java
    ClockProvider.java
```

## Layer Responsibilities

- `config`: Spring configuration and typed properties for RSS, categories, editorial rules, publishing limits, scheduling, cleanup retention, OpenAI, Telegram, profiles, and database settings.
- `scheduler`: scheduled entry points only. Scheduler classes should trigger services and should not contain business logic.
- `service`: application business logic and orchestration: ingestion, source deduplication, semantic event grouping, categorization, source quota selection, summary generation, translation, routing, publication, important news digest publishing, and cleanup.
- `integration`: external system clients and adapters for RSS, OpenAI, and Telegram.
- `repository`: database access through Spring Data repositories.
- `domain.model`: persistent domain entities and core domain objects.
- `domain.enums`: stable domain enums used by entities and services.
- `dto`: boundary objects for RSS, OpenAI, Telegram, and future API responses.
- `mapper`: conversions between DTOs, entities, and message objects.
- `exception`: project-specific exceptions with meaningful failure boundaries.
- `util`: small stateless helpers that do not belong to a specific domain service.

Dependency direction:

```text
scheduler -> service -> repository
scheduler -> service -> integration
integration -> dto
repository -> domain.model
service -> domain.model
service -> dto only at integration boundaries
```

Do not add `controller` packages until the application needs a REST API, admin API, or UI-facing endpoints.

## OpenAI Usage

- Categorization model: `GPT5.5-mini`.
- Translation model: `GPT-5.5`.
- Summary generation model: `GPT-5.5`.
- OpenAI prompts and model names should be configurable.
- OpenAI prompt templates must be stored as text files under `src/main/resources/prompts/`.
- OpenAI JSON request and response contracts are defined in `docs/openai.md`.
- The application configuration must define all possible categories and a separate list of categories selected for publication.
- The application configuration must define free-form editorial classification rules and an editorial rules version.
- The application configuration must define the default max publishable items per source per run and may define per-source overrides.
- The application configuration must define important news digest schedule, importance threshold, and destination channel routing.
- Categorization output should be structured enough to map reliably to configured category codes and semantic event keys.
- Translation and summary output should preserve source meaning and avoid removing source attribution.

## Profiles

The application must support separate runtime profiles:

- `local`: uses SQLite 3 for local development.
- `prod`: uses PostgreSQL for cloud deployments on Kubernetes in AWS.

Profile-specific differences should live in Spring configuration files, not in business logic.

Expected configuration files after project scaffold:

- `application.yml`
- `application-local.yml`
- `application-prod.yml`

## Deployment

- The application should be built as a Docker image.
- Cloud deployment target is a Kubernetes service on AWS.
- Runtime configuration should be supplied through Kubernetes manifests, Helm values, or the selected deployment pipeline.
- Secrets must come from AWS/Kubernetes secret management and must not be baked into the Docker image.
- The container should expose Spring Boot health endpoints suitable for Kubernetes readiness and liveness probes.

## Database Migrations

Liquibase is responsible for database initialization and schema updates.

- Every schema change must be represented as a Liquibase changeset.
- Migrations must be compatible with both SQLite 3 and PostgreSQL where possible.
- When compatibility requires different SQL, isolate database-specific migrations clearly.

## Telegram Publishing Rules

- Each target language must map to one or more Telegram channels.
- Every Telegram message must include source attribution.
- Publication must be idempotent: rerunning a job must not resend the same semantic event to the same language/channel pair.
- Stored publication data must include a Telegram message URL or enough channel metadata to build one.
- Important news digest posts must link titles to already published Telegram posts.
- Publishing failures should be logged and persisted enough to support retry or diagnosis.

## Error Handling

- RSS source failures should not stop the whole processing run.
- Translation failures should not mark a news item as published.
- Telegram API failures should be recorded and retried according to configured policy.
- Cleanup failures should be logged and must not stop ingestion or publication schedulers.
- External calls should have timeouts.

## Areas To Revisit

- OpenAI cost controls and token budgeting.
- Category prompt design and structured output format.
- Retry policy and dead-letter handling.
- Whether cleanup should hard-delete records or archive them first.
- Observability: logs, metrics, and health checks.
