# 011 Translation Generation

## Goal

Translate selected publishable semantic events into configured target languages.

## Scope

- Determine target languages.
- Call GPT-5.5 translation flow.
- Persist translations by semantic event and target language.
- Preserve translated meaning while leaving source attribution to Telegram message mapping.

## Deliverables

- `TranslationService`.
- `OpenAiTranslator`.
- Translation persistence flow.
- Translation prompt usage.

## Tests

- Service tests with mocked OpenAI translator.
- Tests for multiple target languages.
- Tests for existing translation reuse.
- Fixture tests for translation JSON.

## Done When

- Selected semantic events have translations for target languages.
- Translation output provides translated title and summary; Telegram message creation adds source attribution from feed metadata.
