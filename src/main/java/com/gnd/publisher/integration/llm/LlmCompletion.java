package com.gnd.publisher.integration.llm;

/**
 * The raw structured output of a completion, together with the provider and model that produced it.
 *
 * @param outputText the model response as a JSON string, still unparsed
 * @param model      the model that produced the response
 * @param providerId the provider that produced the response
 */
public record LlmCompletion(
        String outputText,
        String model,
        String providerId) {
}
