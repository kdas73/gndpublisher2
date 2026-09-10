package com.gnd.publisher.integration.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnd.publisher.config.LlmProperties;
import com.gnd.publisher.dto.llm.PublicationContentRequest;
import com.gnd.publisher.dto.llm.PublicationContentResponse;
import com.gnd.publisher.exception.LlmIntegrationException;

import org.springframework.stereotype.Component;

/**
 * Publication content use case: resolves the configured provider, requests a schema-constrained
 * completion, then parses and validates it. Unlike classification there is nothing to normalize.
 */
@Component
public class LlmPublicationContentGenerator {

    private static final String SCHEMA_NAME = "publication_content_response";

    private final LlmClientRegistry registry;
    private final LlmProperties properties;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;
    private final LlmResponseValidator validator;

    public LlmPublicationContentGenerator(
            LlmClientRegistry registry,
            LlmProperties properties,
            PromptLoader promptLoader,
            ObjectMapper objectMapper,
            LlmResponseValidator validator) {
        this.registry = registry;
        this.properties = properties;
        this.promptLoader = promptLoader;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public PublicationContentResult prepare(PublicationContentRequest request) {
        LlmProperties.UseCase useCase = properties.publicationContent();
        LlmClient client = registry.require(useCase.provider());

        LlmCompletion completion = client.complete(new LlmCompletionRequest(
                useCase.model(),
                promptLoader.load(properties.prompts().publicationContentVersion()),
                toJson(request),
                SCHEMA_NAME,
                LlmResponseSchemas.publicationContentResponseSchema(),
                properties.readTimeout(useCase.provider())));

        PublicationContentResponse response = parse(completion.outputText());
        validator.validatePublicationContent(response);

        return new PublicationContentResult(response, completion.model(), completion.providerId());
    }

    private String toJson(PublicationContentRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new LlmIntegrationException("Failed to serialize publication content request", exception);
        }
    }

    private PublicationContentResponse parse(String outputText) {
        try {
            return objectMapper.readValue(outputText, PublicationContentResponse.class);
        } catch (JsonProcessingException exception) {
            throw new LlmIntegrationException("Failed to parse LLM output as PublicationContentResponse", exception);
        }
    }

    public record PublicationContentResult(
            PublicationContentResponse response,
            String model,
            String providerId) {
    }
}
