# 007 OpenAI Integration Foundation

## Goal

Create the OpenAI integration foundation without implementing all business flows.

## Scope

- Add prompt loading from `src/main/resources/prompts/`.
- Add prompt template files.
- Add DTOs from `docs/openai.md`.
- Add OpenAI client abstraction.
- Add response parsing and validation.
- Do not call OpenAI from unit tests.

## Deliverables

- `OpenAiClient`.
- Prompt loader component.
- `classification-v1.txt`.
- `summary-v1.txt`.
- `translation-v1.txt`.
- OpenAI DTOs under `com.gnd.publisher.dto.openai`.

## Tests

- Prompt loader unit tests.
- JSON fixture deserialization tests.
- OpenAI client tests with mock HTTP server or mocked low-level client.

## Done When

- Prompt templates are loaded from resources.
- DTOs match `docs/openai.md`.
- No prompt text is hardcoded in Java services.
