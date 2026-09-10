package com.gnd.publisher.integration.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gnd.publisher.dto.llm.CategoryClassificationRequest;
import com.gnd.publisher.dto.llm.CategoryOptionDto;
import com.gnd.publisher.dto.llm.LlmNewsItemDto;
import com.gnd.publisher.dto.llm.SemanticEventKeyCandidateDto;
import com.gnd.publisher.dto.llm.SemanticKeyActionDto;
import com.gnd.publisher.exception.LlmIntegrationException;

import org.junit.jupiter.api.Test;

class LlmCategorizerTest {

    private static final String CLASSIFICATION_OUTPUT = """
            {
              "primaryCategoryCode": "politics",
              "categoryCodes": ["politics"],
              "semanticKey": "greek parliament approves migration bill",
              "semanticKeyAction": "matched_existing",
              "matchedSemanticEventId": "event-456",
              "confidence": 0.91,
              "shouldPublish": true,
              "rejectionReason": null
            }""";

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final PromptLoader promptLoader = new PromptLoader();

    @Test
    void buildsTheRequestFromTheConfiguredUseCaseAndReturnsTheProducingProvider() {
        RecordingLlmClient client = new RecordingLlmClient("openai", CLASSIFICATION_OUTPUT);

        LlmCategorizer.CategorizationResult result = categorizer(client, "openai").classify(request());

        assertThat(result.response().semanticKey()).isEqualTo("greek parliament approves migration bill");
        assertThat(result.response().semanticKeyAction()).isEqualTo(SemanticKeyActionDto.MATCHED_EXISTING);
        assertThat(result.rawResponse()).contains("semanticKey");
        assertThat(result.model()).isEqualTo("gpt-5.4-mini");
        assertThat(result.providerId()).isEqualTo("openai");

        LlmCompletionRequest sent = client.lastRequest();
        assertThat(sent.model()).isEqualTo("gpt-5.4-mini");
        assertThat(sent.schemaName()).isEqualTo("category_classification_response");
        assertThat(sent.instructions()).contains("strict category relevance classifier");
        assertThat(sent.jsonSchema()).isEqualTo(LlmResponseSchemas.classificationResponseSchema());
        assertThat(sent.inputJson()).contains("ERT News");
        assertThat(sent.readTimeout()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    void resolvesTheOllamaProviderAndItsReadTimeoutWhenConfigured() {
        RecordingLlmClient client = new RecordingLlmClient("ollama", CLASSIFICATION_OUTPUT);

        LlmCategorizer.CategorizationResult result = categorizer(client, "ollama").classify(request());

        assertThat(result.providerId()).isEqualTo("ollama");
        assertThat(client.lastRequest().readTimeout()).isEqualTo(Duration.ofSeconds(300));
    }

    @Test
    void normalizesBeforeValidatingSoARepairableResponseSurvives() {
        // created_new with a matched id is repaired rather than rejected.
        RecordingLlmClient client = new RecordingLlmClient("openai", """
                {
                  "primaryCategoryCode": "politics",
                  "categoryCodes": [],
                  "semanticKey": "greek parliament approves migration bill",
                  "semanticKeyAction": "created_new",
                  "matchedSemanticEventId": "event-456",
                  "confidence": 0.91,
                  "shouldPublish": true,
                  "rejectionReason": null
                }""");

        LlmCategorizer.CategorizationResult result = categorizer(client, "openai").classify(request());

        assertThat(result.response().matchedSemanticEventId()).isNull();
        assertThat(result.response().categoryCodes()).containsExactly("politics");
        // The raw response is preserved for audit, un-normalized.
        assertThat(result.rawResponse()).contains("event-456");
    }

    @Test
    void propagatesValidationFailures() {
        RecordingLlmClient client = new RecordingLlmClient("openai", """
                {
                  "primaryCategoryCode": "sports",
                  "categoryCodes": ["sports"],
                  "semanticKey": "unrelated",
                  "semanticKeyAction": "created_new",
                  "matchedSemanticEventId": null,
                  "confidence": 0.91,
                  "shouldPublish": false,
                  "rejectionReason": "NOT_PUBLISHABLE_CATEGORY"
                }""");

        assertThatThrownBy(() -> categorizer(client, "openai").classify(request()))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("not configured");
    }

    @Test
    void failsForAnUnparseableOutput() {
        RecordingLlmClient client = new RecordingLlmClient("openai", "not json");

        assertThatThrownBy(() -> categorizer(client, "openai").classify(request()))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("Failed to parse LLM output as CategoryClassificationResponse");
    }

    private LlmCategorizer categorizer(LlmClient client, String provider) {
        return new LlmCategorizer(
                new LlmClientRegistry(List.of(client)),
                LlmTestProperties.properties(provider, provider),
                promptLoader,
                objectMapper,
                new LlmResponseNormalizer(),
                new LlmResponseValidator());
    }

    private CategoryClassificationRequest request() {
        return new CategoryClassificationRequest(
                new LlmNewsItemDto(
                        "Greek parliament approves new migration bill",
                        "Greek lawmakers approved a new migration bill after a lengthy debate.",
                        "ERT News",
                        "https://example.gr/news/item",
                        Instant.parse("2026-07-01T10:15:00Z")),
                List.of(
                        new CategoryOptionDto("politics", "Government, elections, laws, public administration"),
                        new CategoryOptionDto("weather", "Weather alerts, climate events, natural hazards")),
                List.of("politics"),
                List.of("For sports news, publish only items with concrete match results."),
                List.of(new SemanticEventKeyCandidateDto(
                        "event-456",
                        "greek parliament debates migration bill")));
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
