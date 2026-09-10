package com.gnd.publisher.integration.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import com.gnd.publisher.exception.LlmIntegrationException;

import org.junit.jupiter.api.Test;

class LlmClientRegistryTest {

    private final LlmClient openAi = new StubLlmClient("openai");
    private final LlmClient ollama = new StubLlmClient("ollama");

    @Test
    void resolvesClientByProviderId() {
        LlmClientRegistry registry = new LlmClientRegistry(List.of(openAi, ollama));

        assertThat(registry.require("openai")).isSameAs(openAi);
        assertThat(registry.require("ollama")).isSameAs(ollama);
        assertThat(registry.providerIds()).containsExactlyInAnyOrder("openai", "ollama");
    }

    @Test
    void resolvesRegardlessOfConfiguredCasingAndWhitespace() {
        LlmClientRegistry registry = new LlmClientRegistry(List.of(openAi, ollama));

        assertThat(registry.require("OLLAMA")).isSameAs(ollama);
        assertThat(registry.require("  OpenAI ")).isSameAs(openAi);
    }

    @Test
    void failsForUnknownProviderIdAndNamesTheKnownOnes() {
        LlmClientRegistry registry = new LlmClientRegistry(List.of(openAi, ollama));

        assertThatThrownBy(() -> registry.require("anthropic"))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("Unknown LLM provider id: anthropic")
                .hasMessageContaining("openai")
                .hasMessageContaining("ollama");
    }

    @Test
    void failsForNullProviderId() {
        LlmClientRegistry registry = new LlmClientRegistry(List.of(openAi, ollama));

        assertThatThrownBy(() -> registry.require(null))
                .isInstanceOf(LlmIntegrationException.class);
    }

    @Test
    void rejectsDuplicateProviderIds() {
        assertThatThrownBy(() -> new LlmClientRegistry(List.of(openAi, new StubLlmClient("openai"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate LLM provider id");
    }

    private record StubLlmClient(String providerId) implements LlmClient {

        @Override
        public LlmCompletion complete(LlmCompletionRequest request) {
            return new LlmCompletion("{}", request.model(), providerId);
        }
    }
}
