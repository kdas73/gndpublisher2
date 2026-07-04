# OpenAI JSON Contracts

This document defines the minimal model-facing JSON contracts used in OpenAI prompts and structured outputs. Keep these schemas small: the model should receive only fields that help it decide or generate text.

These payloads are application DTO contracts, not a direct copy of the raw OpenAI HTTP API format.

## General Rules

- Use strict JSON only. Responses must not contain Markdown, comments, prose outside JSON, or trailing commas.
- Use lowerCamelCase field names.
- Keep model-facing schemas minimal.
- Do not ask the model to echo technical metadata that the application already knows.
- Unknown enum values must be treated as integration errors.
- Nullable values must be represented as `null`, not omitted, when the field is part of the contract.
- Confidence values must be numbers from `0.0` to `1.0`.

## Application Metadata

The application owns technical metadata and persists it outside the model-facing JSON.

Application-managed metadata includes:

- `requestId`
- `model`
- `promptVersion`
- lookup window start and end timestamps
- candidate key count
- source `newsItemId`
- selected `semanticEventId`
- `editorialRulesVersion`
- request timestamp
- raw model response

Store classifier raw responses in `classification_runs.raw_response`, but do not log them casually.

## Prompt Storage

All OpenAI prompt templates must be stored as text files in application resources, not hardcoded in Java classes.

Recommended location:

```text
src/main/resources/prompts/
```

Initial prompt files:

```text
src/main/resources/prompts/classification-v1.txt
src/main/resources/prompts/editorial-rules-v1.txt
src/main/resources/prompts/publication-content-v1.txt
```

Rules:

- The prompt file name should match the application-managed `promptVersion`.
- The editorial rules file name should match the application-managed `editorialRulesVersion`.
- Prompt templates should be loaded through a dedicated prompt-loading component in the OpenAI integration layer.
- Prompt text changes must update the corresponding `promptVersion` when behavior changes.
- Prompt files may contain template placeholders, but JSON payloads must remain validated DTOs.
- Do not store secrets, API keys, Telegram channel IDs, or environment-specific credentials in prompt files.

## Classification And Semantic Event Grouping

Model: `GPT5.5-mini`

Purpose:

- assign a news item to configured categories;
- select an existing semantic event key from recent candidates or create a new semantic key;
- decide whether the semantic event should be published based on configured publishable categories.
- apply free-form editorial rules that refine or exclude publication decisions.

The application must limit candidate semantic events by a configurable lookup time window before building the prompt.
The application must pass all configured categories in `categoryOptions` and the publication allow-list in `publishableCategoryCodes`.
The application must pass free-form editorial rules in `editorialRules` when they are configured.

### Classification Request

```json
{
  "newsItem": {
    "title": "Greek parliament approves new migration bill",
    "summary": "Greek lawmakers approved a new migration bill after a lengthy debate.",
    "sourceName": "ERT News",
    "sourceUrl": "https://example.gr/news/item",
    "publishedAt": "2026-07-01T10:15:00Z"
  },
  "categoryOptions": [
    {
      "code": "politics",
      "description": "Government, elections, laws, public administration"
    },
    {
      "code": "weather",
      "description": "Weather alerts, climate events, natural hazards"
    }
  ],
  "publishableCategoryCodes": ["politics"],
  "editorialRules": [
    "For sports news, publish only items with concrete match results.",
    "Ignore celebrity death news unless it has direct political or public-safety impact."
  ],
  "candidateSemanticEvents": [
    {
      "id": "event-456",
      "semanticKey": "greek parliament debates migration bill"
    }
  ]
}
```

### Classification Response

```json
{
  "primaryCategoryCode": "politics",
  "categoryCodes": ["politics"],
  "semanticKey": "greek parliament approves new migration bill",
  "semanticKeyAction": "matched_existing",
  "matchedSemanticEventId": "event-456",
  "confidence": 0.91,
  "shouldPublish": true,
  "rejectionReason": null
}
```

Allowed `semanticKeyAction` values:

- `matched_existing`
- `created_new`

Rules:

- When `semanticKeyAction` is `matched_existing`, `matchedSemanticEventId` must contain an id from `candidateSemanticEvents`.
- When `semanticKeyAction` is `created_new`, `matchedSemanticEventId` must be `null`.
- `semanticKey` must be a short normalized phrase, not a hash.
- `semanticKey` must describe the real-world event, not the article wording.
- `categoryCodes` must contain only codes from `categoryOptions`.
- `shouldPublish` must be `true` only when at least one selected category is present in `publishableCategoryCodes`.
- `shouldPublish` must be `false` when the item is excluded by `editorialRules`.
- `rejectionReason` should be `EDITORIAL_RULE_EXCLUDED` when an editorial rule excludes the item.
- `rejectionReason` should be `NOT_PUBLISHABLE_CATEGORY` when no selected category is present in `publishableCategoryCodes`.

## Publication Content Generation

Model: `GPT-5.5`

Purpose:

- create or improve a concise publication-ready summary when the RSS summary is missing, too short, noisy, or unsuitable for Telegram publishing;
- translate or write the selected publishable semantic event content into a target language;
- use classification context to improve emphasis and disambiguation without inventing facts;
- preserve meaning, names, dates, and locations;
- leave source attribution and Telegram message formatting to application code, using feed metadata.

This request runs only after classification, semantic event grouping, and source quota selection.

### Publication Content Request

```json
{
  "title": "Greek parliament approves new migration bill",
  "summary": "Greek lawmakers approved a new migration bill after a lengthy debate.",
  "sourceName": "ERT News",
  "sourceUrl": "https://example.gr/news/item",
  "sourceLanguage": "el",
  "targetLanguage": "en",
  "semanticKey": "greek parliament approves new migration bill",
  "primaryCategoryCode": "politics",
  "categoryCodes": ["politics"],
  "maxSummaryCharacters": 600,
  "maxMessageCharacters": 3500
}
```

### Publication Content Response

```json
{
  "title": "Greek parliament approves new migration bill",
  "summary": "Greek lawmakers approved a new migration bill after a lengthy parliamentary debate.",
  "confidence": 0.9
}
```

Rules:

- Do not add facts that are not present in the source material.
- Preserve important names, locations, dates, and official entities.
- `title` and `summary` must be in `targetLanguage`.
- `semanticKey`, `primaryCategoryCode`, and `categoryCodes` are context for emphasis and disambiguation, not new source facts.
- Keep `summary` concise and within `maxSummaryCharacters`.
- The final Telegram message with attribution must fit within `maxMessageCharacters` after application formatting.
- Do not include source attribution, source URLs, or Telegram formatting in the model response.
- Source attribution must be added later by application message mapping from feed metadata.

## Validation And Persistence Mapping

- Classification responses map to `classification_runs`, `semantic_news_events`, `news_items`, and `news_item_categories`.
- Publication content responses map to `translations` for target-language publication content. `news_summaries` may be used later only if the application needs to persist source-language canonical summaries separately.
- Telegram publishing formats the final message from persisted title, summary, and source metadata; source attribution is generated by application code.
- All OpenAI request and response DTOs should live under `com.gnd.publisher.dto.openai`.
