package com.gnd.publisher.integration.llm;

import java.time.Duration;

import com.gnd.publisher.config.LlmProperties;

/** Shared {@link LlmProperties} fixture for the provider-agnostic use-case tests. */
final class LlmTestProperties {

    private LlmTestProperties() {
    }

    static LlmProperties properties(String categorizationProvider, String publicationContentProvider) {
        return new LlmProperties(
                new LlmProperties.UseCase(categorizationProvider, "gpt-5.4-mini"),
                new LlmProperties.UseCase(publicationContentProvider, "gpt-5.5"),
                new LlmProperties.Prompts(
                        "classification-v1",
                        "editorial-rules-v1",
                        "publication-content-v1"),
                new LlmProperties.OpenAi(
                        "test-api-key",
                        "https://api.openai.com/v1",
                        new LlmProperties.Timeouts(Duration.ofSeconds(5), Duration.ofSeconds(60))),
                new LlmProperties.Ollama(
                        "http://localhost:11434",
                        new LlmProperties.Timeouts(Duration.ofSeconds(5), Duration.ofSeconds(300)),
                        "10m",
                        new LlmProperties.Options(0.0)));
    }
}
