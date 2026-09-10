# 017 LLM Provider Abstraction

## Goal

Introduce a provider-neutral LLM abstraction so classification and publication content generation no longer depend on the OpenAI client, and add a second implementation targeting a local Ollama API.

## Scope

- Replace the domain-shaped `OpenAiClient` port with a transport-level `LlmClient` port that exchanges instructions, input JSON, and a response JSON Schema.
- Move JSON Schema construction, response parsing, normalization, and validation out of `HttpOpenAiClient` into a provider-agnostic layer shared by all providers.
- Keep the existing OpenAI behaviour intact by reimplementing it as an adapter over the Responses API.
- Add an Ollama adapter using the native `/api/chat` endpoint with `format` structured outputs.
- Select the provider per use case through configuration, defaulting both use cases to OpenAI.
- Resolve the provider through a registry keyed by provider id rather than by wiring a single adapter bean, so both adapters stay loaded and the choice is data.
- Add an `ollama` Spring profile that points both use cases at a local Ollama instance.
- Rename provider-specific packages, DTOs, and the integration exception to provider-neutral names.
- Persist the actual provider that produced each classification and translation instead of hardcoded literals.
- Move `semantic-event-lookup-window` out of provider configuration into classification configuration.
- Make `SemanticKeyActionDto` and `ClassificationRejectionReasonDto` enum parsing case-insensitive, because smaller local models vary the casing of enum values.
- Require an OpenAI API key only when a use case actually targets OpenAI.
- Update the reference documentation that names OpenAI as the only provider.
- Leave completed task documents 002 through 016 unchanged as historical records.
- Do not add retries, provider fallback, token accounting, or streaming.
- Do not add a database migration, because the existing provider columns already fit the new values.

## Target Structure

```text
integration/llm/
  LlmClient                         port: providerId(), complete(LlmCompletionRequest)
  LlmCompletionRequest              model, instructions, inputJson, schemaName, jsonSchema, readTimeout
  LlmCompletion                     outputText, model, providerId
  LlmClientRegistry                 resolves an LlmClient by configured provider id
  LlmHttpSender / JdkLlmHttpSender  transport seam, one bean per provider connect timeout
  LlmCategorizer                    schema, parse, normalize, and validate for classification
  LlmPublicationContentGenerator    schema, parse, and validate for publication content
  LlmResponseSchemas                JSON Schema builders lifted from HttpOpenAiClient
  LlmResponseNormalizer             normalizeClassificationResponse and its helpers
  LlmResponseValidator              renamed OpenAiResponseValidator
  PromptLoader                      moved unchanged
integration/llm/openai/OpenAiLlmClient    Responses API wire format only
integration/llm/ollama/OllamaLlmClient    /api/chat wire format only
```

Renames: `dto/openai` to `dto/llm`, `OpenAiNewsItemDto` to `LlmNewsItemDto`, `OpenAiIntegrationException` to `LlmIntegrationException`, and fixtures `fixtures/openai` to `fixtures/llm`. The `integration/openai` package is deleted. `PromptLoader` and `OpenAiResponseValidator` are already provider-agnostic and only change package.

## Configuration

```text
gnd.openai.api-key                       -> gnd.llm.openai.api-key
gnd.openai.timeouts.*                    -> gnd.llm.openai.timeouts.*
gnd.openai.models.categorization         -> gnd.llm.categorization.model
gnd.openai.models.publication-content    -> gnd.llm.publication-content.model
gnd.openai.prompts.*                     -> gnd.llm.prompts.*
gnd.openai.semantic-event-lookup-window  -> gnd.classification.semantic-event-lookup-window
new: gnd.llm.categorization.provider, gnd.llm.publication-content.provider
new: gnd.llm.openai.base-url, gnd.llm.ollama.base-url, gnd.llm.ollama.timeouts.*,
     gnd.llm.ollama.keep-alive, gnd.llm.ollama.options.temperature
```

Switching providers requires no source change and no file edit in a deployment, because Spring relaxed binding accepts environment variables such as `GND_LLM_CATEGORIZATION_PROVIDER=ollama`. For local runs the `ollama` profile switches both use cases at once through `--spring.profiles.active=local,ollama`. The change takes effect on restart, and provider selection is deliberately not hot-swappable.

`application-secrets.local.yml` is not tracked in the repository, so this migration cannot update it. Rename its `gnd.openai.api-key` entry to `gnd.llm.openai.api-key` manually, otherwise the application fails to start.

## Deliverables

- `LlmClient`, `LlmCompletionRequest`, `LlmCompletion`, `LlmClientRegistry`.
- `LlmCategorizer`, `LlmPublicationContentGenerator`, `LlmResponseSchemas`, `LlmResponseNormalizer`, `LlmResponseValidator`.
- `OpenAiLlmClient` and `OllamaLlmClient` adapters over a shared `LlmHttpSender`.
- `LlmProperties` and `ClassificationProperties`, replacing `OpenAiProperties`.
- `application-ollama.yml` profile selecting Ollama for both use cases.
- Provider id recorded on `NewsItemCategory` classifier matches and on `Translation` rows.
- `docs/llm.md` replacing `docs/openai.md`, holding the provider-neutral JSON contract plus a wire format section per provider.
- `docs/architecture.md` with the LLM section, package structure, and layer responsibilities updated.
- `docs/overview.md` and `docs/db.md` describing the producing component by role rather than by model name.
- `docs/observability.md` with the renamed exception, adapter classes, and secret-leak tests.
- `docs/security.md` covering the Ollama base URL as configuration that must point at an internal host.
- `docs/development.md` with the moved fixture path and instructions for running against local Ollama.
- `AGENTS.md` and `docs/roadmap.md` updated.

## Tests

- Adapter tests for `OpenAiLlmClient` asserting the Responses API URI, bearer header, strict `json_schema` format, and configurable base URL.
- Adapter tests for `OllamaLlmClient` asserting the `/api/chat` URI, disabled streaming, the schema passed as `format`, absence of an authorization header, and parsing of `message.content`.
- Adapter tests asserting that Ollama `error` payloads and a non-stop `done_reason` raise `LlmIntegrationException`.
- Tests that normalization and validation behave identically regardless of provider.
- `LlmClientRegistry` tests for resolution by provider id and failure on an unknown id.
- Configuration binding tests for the new prefixes, including an Ollama-only setup with no API key.
- Enum parsing tests covering unexpected casing from local models.
- Service tests asserting that the persisted provider id follows the configured provider.
- Existing secret-leak tests kept for both adapters.

## Done When

- No class outside `integration/llm/openai` and `integration/llm/ollama` references a concrete provider.
- Switching a use case between `openai` and `ollama` requires only a configuration change and a restart.
- The application starts and runs with no OpenAI API key when both use cases target Ollama.
- Classification and publication content produce equivalent results through both providers.
- Persisted rows record the provider that actually produced them.
- `.\gradlew.bat build` passes.
