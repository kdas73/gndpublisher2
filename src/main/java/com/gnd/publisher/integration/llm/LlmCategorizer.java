package com.gnd.publisher.integration.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnd.publisher.config.LlmProperties;
import com.gnd.publisher.dto.llm.CategoryClassificationRequest;
import com.gnd.publisher.dto.llm.CategoryClassificationResponse;
import com.gnd.publisher.exception.LlmIntegrationException;

import org.springframework.stereotype.Component;

/**
 * Classification use case: resolves the configured provider, requests a schema-constrained
 * completion, then parses, normalizes, and validates it. Everything except the wire format is
 * provider-agnostic, so results are equivalent whichever provider is configured.
 */
@Component
public class LlmCategorizer {

    private static final String SCHEMA_NAME = "category_classification_response";

    private final LlmClientRegistry registry;
    private final LlmProperties properties;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;
    private final LlmResponseNormalizer normalizer;
    private final LlmResponseValidator validator;

    public LlmCategorizer(
            LlmClientRegistry registry,
            LlmProperties properties,
            PromptLoader promptLoader,
            ObjectMapper objectMapper,
            LlmResponseNormalizer normalizer,
            LlmResponseValidator validator) {
        this.registry = registry;
        this.properties = properties;
        this.promptLoader = promptLoader;
        this.objectMapper = objectMapper;
        this.normalizer = normalizer;
        this.validator = validator;
    }

    public CategorizationResult classify(CategoryClassificationRequest request) {
        LlmProperties.UseCase useCase = properties.categorization();
        LlmClient client = registry.require(useCase.provider());

        LlmCompletion completion = client.complete(new LlmCompletionRequest(
                useCase.model(),
                promptLoader.load(properties.prompts().classificationVersion()),
                toJson(request),
                SCHEMA_NAME,
                LlmResponseSchemas.classificationResponseSchema(),
                properties.readTimeout(useCase.provider())));

        CategoryClassificationResponse parsed = parse(completion.outputText());
        CategoryClassificationResponse response = normalizer.normalizeClassificationResponse(request, parsed);
        validator.validateClassification(request, response);

        return new CategorizationResult(
                response,
                completion.outputText(),
                completion.model(),
                completion.providerId());
    }

    private String toJson(CategoryClassificationRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new LlmIntegrationException("Failed to serialize classification request", exception);
        }
    }

    private CategoryClassificationResponse parse(String outputText) {
        try {
            return objectMapper.readValue(outputText, CategoryClassificationResponse.class);
        } catch (JsonProcessingException exception) {
            throw new LlmIntegrationException("Failed to parse LLM output as CategoryClassificationResponse",
                    exception);
        }
    }

    public record CategorizationResult(
            CategoryClassificationResponse response,
            String rawResponse,
            String model,
            String providerId) {
    }
}
