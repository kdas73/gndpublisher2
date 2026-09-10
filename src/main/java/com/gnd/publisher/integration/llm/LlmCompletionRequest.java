package com.gnd.publisher.integration.llm;

import java.time.Duration;
import java.util.Map;

/**
 * A provider-neutral structured completion request.
 *
 * @param model       provider-specific model identifier
 * @param instructions system-level prompt text
 * @param inputJson   the request DTO serialized as a JSON string
 * @param schemaName  name for the response schema; only OpenAI carries it on the wire, other
 *                    providers legitimately ignore it
 * @param jsonSchema  JSON Schema the provider must constrain its response to
 * @param readTimeout per-provider read timeout applied to the HTTP request
 */
public record LlmCompletionRequest(
        String model,
        String instructions,
        String inputJson,
        String schemaName,
        Map<String, Object> jsonSchema,
        Duration readTimeout) {
}
