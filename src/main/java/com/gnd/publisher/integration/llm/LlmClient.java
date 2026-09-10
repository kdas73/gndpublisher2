package com.gnd.publisher.integration.llm;

/**
 * Transport-level port for a large language model provider.
 *
 * <p>Implementations translate a provider-neutral {@link LlmCompletionRequest} into a single
 * provider wire format and back. Schema construction, response normalization, and validation
 * live outside this port so that every provider shares them.
 */
public interface LlmClient {

    /**
     * Canonical lower-case provider id, matching the values accepted by
     * {@code gnd.llm.categorization.provider} and {@code gnd.llm.publication-content.provider}.
     */
    String providerId();

    LlmCompletion complete(LlmCompletionRequest request);
}
