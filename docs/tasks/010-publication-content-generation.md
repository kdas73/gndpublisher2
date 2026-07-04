# 010 Publication Content Generation

## Goal

Generate target-language publication content for selected publishable semantic events in one OpenAI call.

## Scope

- Determine target languages.
- Call GPT-5.5 publication content flow only for selected candidates.
- Include semantic key and category context in the request.
- Create or improve concise summaries as part of target-language content generation.
- Persist target-language publication content by semantic event and target language.
- Reuse existing publication content when it already exists for a semantic event and target language.
- Preserve meaning while leaving source attribution to Telegram message mapping.
- Avoid adding unsupported facts.

## Deliverables

- `PublicationContentService`.
- `OpenAiPublicationContentGenerator`.
- Publication content persistence flow.
- Publication content prompt usage.

## Tests

- Service tests with mocked OpenAI publication content generator.
- Tests for multiple target languages.
- Tests for existing publication content reuse.
- Fixture tests for publication content JSON.

## Done When

- Selected semantic events have target-language titles and summaries.
- Publication content generation is skipped for non-selected candidates.
- Telegram message creation can add source attribution from feed metadata.
