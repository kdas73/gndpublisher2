package com.gnd.publisher.integration.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnd.publisher.dto.llm.PublicationContentRequest;
import com.gnd.publisher.exception.LlmIntegrationException;

import org.junit.jupiter.api.Test;

class LlmPublicationContentGeneratorTest {

    private static final String CONTENT_OUTPUT = """
            {
              "title": "Greek parliament approves new migration bill",
              "summary": "Greek lawmakers approved a new migration bill after a lengthy parliamentary debate.",
              "confidence": 0.9
            }""";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final PromptLoader promptLoader = new PromptLoader();

    @Test
    void buildsTheRequestFromTheConfiguredUseCaseAndReturnsTheProducingProvider() {
        RecordingLlmClient client = new RecordingLlmClient("openai", CONTENT_OUTPUT);

        LlmPublicationContentGenerator.PublicationContentResult result =
                generator(client, "openai").prepare(request());

        assertThat(result.response().summary()).contains("migration bill");
        assertThat(result.model()).isEqualTo("gpt-5.5");
        assertThat(result.providerId()).isEqualTo("openai");

        LlmCompletionRequest sent = client.lastRequest();
        assertThat(sent.model()).isEqualTo("gpt-5.5");
        assertThat(sent.schemaName()).isEqualTo("publication_content_response");
        assertThat(sent.instructions()).contains("publisher of selected content");
        assertThat(sent.jsonSchema()).isEqualTo(LlmResponseSchemas.publicationContentResponseSchema());
        assertThat(sent.inputJson()).contains("maxSummaryCharacters");
        assertThat(sent.readTimeout()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void producesAnEquivalentResultThroughOllama() {
        RecordingLlmClient client = new RecordingLlmClient("ollama", CONTENT_OUTPUT);

        LlmPublicationContentGenerator.PublicationContentResult result =
                generator(client, "ollama").prepare(request());

        assertThat(result.response().title()).isEqualTo("Greek parliament approves new migration bill");
        assertThat(result.response().summary()).contains("parliamentary debate");
        assertThat(result.providerId()).isEqualTo("ollama");
        assertThat(client.lastRequest().readTimeout()).isEqualTo(Duration.ofSeconds(300));
    }

    @Test
    void propagatesValidationFailures() {
        RecordingLlmClient client = new RecordingLlmClient("openai", """
                {"title": "Title", "summary": "Summary", "confidence": 1.4}""");

        assertThatThrownBy(() -> generator(client, "openai").prepare(request()))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("confidence must be between 0.0 and 1.0");
    }

    @Test
    void failsForAnUnparseableOutput() {
        RecordingLlmClient client = new RecordingLlmClient("openai", "not json");

        assertThatThrownBy(() -> generator(client, "openai").prepare(request()))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("Failed to parse LLM output as PublicationContentResponse");
    }

    private LlmPublicationContentGenerator generator(LlmClient client, String provider) {
        return new LlmPublicationContentGenerator(
                new LlmClientRegistry(List.of(client)),
                LlmTestProperties.properties(provider, provider),
                promptLoader,
                objectMapper,
                new LlmResponseValidator());
    }

    private PublicationContentRequest request() {
        return new PublicationContentRequest(
                "Greek parliament approves new migration bill",
                "Greek lawmakers approved a new migration bill after a lengthy parliamentary debate.",
                "ERT News",
                "https://example.gr/news/item",
                "el",
                "en",
                "greek parliament approves new migration bill",
                "politics",
                List.of("politics"),
                600,
                3500);
    }

    private static final class RecordingLlmClient implements LlmClient {

        private final String providerId;
        private final String outputText;
        private LlmCompletionRequest lastRequest;

        private RecordingLlmClient(String providerId, String outputText) {
            this.providerId = providerId;
            this.outputText = outputText;
        }

        @Override
        public String providerId() {
            return providerId;
        }

        @Override
        public LlmCompletion complete(LlmCompletionRequest request) {
            this.lastRequest = request;
            return new LlmCompletion(outputText, request.model(), providerId);
        }

        private LlmCompletionRequest lastRequest() {
            return lastRequest;
        }
    }
}
