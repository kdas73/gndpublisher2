# 011 Translation Generation

## Goal

Translate selected publishable semantic events into configured target languages.

## Scope

- Determine target languages.
- Call GPT-5.5 translation flow.
- Persist translations by semantic event and target language.
- Preserve source attribution requirements.

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
- Translation output includes enough data for Telegram message creation.
