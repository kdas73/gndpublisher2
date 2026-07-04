package com.gnd.publisher.integration.openai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnd.publisher.config.OpenAiProperties;
import com.gnd.publisher.dto.openai.CategoryClassificationRequest;
import com.gnd.publisher.dto.openai.CategoryClassificationResponse;
import com.gnd.publisher.dto.openai.SummaryRequest;
import com.gnd.publisher.dto.openai.SummaryResponse;
import com.gnd.publisher.dto.openai.TranslationRequest;
import com.gnd.publisher.dto.openai.TranslationResponse;
import com.gnd.publisher.exception.OpenAiIntegrationException;

import org.springframework.stereotype.Component;

@Component
public class HttpOpenAiClient implements OpenAiClient {

    private static final URI RESPONSES_URI = URI.create("https://api.openai.com/v1/responses");

    private final OpenAiProperties properties;
    private final PromptLoader promptLoader;
    private final ObjectMapper objectMapper;
    private final OpenAiHttpSender httpSender;
    private final OpenAiResponseValidator validator;

    public HttpOpenAiClient(
            OpenAiProperties properties,
            PromptLoader promptLoader,
            ObjectMapper objectMapper,
            OpenAiHttpSender httpSender,
            OpenAiResponseValidator validator) {
        this.properties = properties;
        this.promptLoader = promptLoader;
        this.objectMapper = objectMapper;
        this.httpSender = httpSender;
        this.validator = validator;
    }

    @Override
    public CategoryClassificationResponse classify(CategoryClassificationRequest request) {
        CategoryClassificationResponse response = execute(
                properties.models().categorization(),
                properties.prompts().classificationVersion(),
                "category_classification_response",
                classificationResponseSchema(),
                request,
                CategoryClassificationResponse.class);
        validator.validateClassification(request, response);
        return response;
    }

    @Override
    public SummaryResponse summarize(SummaryRequest request) {
        SummaryResponse response = execute(
                properties.models().summary(),
                properties.prompts().summaryVersion(),
                "summary_response",
                summaryResponseSchema(),
                request,
                SummaryResponse.class);
        validator.validateSummary(response);
        return response;
    }

    @Override
    public TranslationResponse translate(TranslationRequest request) {
        TranslationResponse response = execute(
                properties.models().translation(),
                properties.prompts().translationVersion(),
                "translation_response",
                translationResponseSchema(),
                request,
                TranslationResponse.class);
        validator.validateTranslation(response);
        return response;
    }

    private <T> T execute(
            String model,
            String promptVersion,
            String schemaName,
            Map<String, Object> schema,
            Object input,
            Class<T> responseType) {
        String prompt = promptLoader.load(promptVersion);
        String inputJson = toJson(input);
        String requestJson = toJson(openAiRequest(model, prompt, inputJson, schemaName, schema));

        HttpRequest httpRequest = HttpRequest.newBuilder(RESPONSES_URI)
                .timeout(properties.timeouts().read())
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> httpResponse = send(httpRequest);
        if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
            throw new OpenAiIntegrationException("OpenAI returned HTTP status " + httpResponse.statusCode());
        }

        String outputText = extractOutputText(httpResponse.body());
        try {
            return objectMapper.readValue(outputText, responseType);
        } catch (JsonProcessingException exception) {
            throw new OpenAiIntegrationException("Failed to parse OpenAI output as " + responseType.getSimpleName(),
                    exception);
        }
    }

    private HttpResponse<String> send(HttpRequest httpRequest) {
        try {
            return httpSender.send(httpRequest);
        } catch (IOException exception) {
            throw new OpenAiIntegrationException("Failed to call OpenAI", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OpenAiIntegrationException("Interrupted while calling OpenAI", exception);
        }
    }

    private String extractOutputText(String responseBody) {
        JsonNode root = parseResponseBody(responseBody);
        JsonNode error = root.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            throw new OpenAiIntegrationException("OpenAI response contained an error: "
                    + error.path("message").asText("unknown error"));
        }
        JsonNode incompleteDetails = root.path("incomplete_details");
        if (!incompleteDetails.isMissingNode() && !incompleteDetails.isNull()) {
            throw new OpenAiIntegrationException("OpenAI response was incomplete: "
                    + incompleteDetails.path("reason").asText("unknown reason"));
        }

        JsonNode output = root.path("output");
        if (!output.isArray()) {
            throw new OpenAiIntegrationException("OpenAI response did not contain output array");
        }
        for (JsonNode outputItem : output) {
            JsonNode content = outputItem.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                if ("output_text".equals(contentItem.path("type").asText())) {
                    String text = contentItem.path("text").asText();
                    if (!text.isBlank()) {
                        return text;
                    }
                }
            }
        }
        throw new OpenAiIntegrationException("OpenAI response did not contain output_text");
    }

    private JsonNode parseResponseBody(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            throw new OpenAiIntegrationException("Failed to parse OpenAI response body", exception);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new OpenAiIntegrationException("Failed to serialize OpenAI payload", exception);
        }
    }

    private Map<String, Object> openAiRequest(
            String model,
            String instructions,
            String inputJson,
            String schemaName,
            Map<String, Object> schema) {
        return Map.of(
                "model", model,
                "instructions", instructions,
                "input", inputJson,
                "text", Map.of(
                        "format", Map.of(
                                "type", "json_schema",
                                "name", schemaName,
                                "strict", true,
                                "schema", schema)),
                "tools", List.of(),
                "store", false);
    }

    private Map<String, Object> classificationResponseSchema() {
        return objectSchema(
                Map.of(
                        "primaryCategoryCode", stringSchema(),
                        "categoryCodes", Map.of(
                                "type", "array",
                                "items", stringSchema()),
                        "semanticKey", stringSchema(),
                        "semanticKeyAction", Map.of(
                                "type", "string",
                                "enum", List.of("matched_existing", "created_new")),
                        "matchedSemanticEventId", nullableStringSchema(),
                        "confidence", numberSchema(),
                        "shouldPublish", Map.of("type", "boolean"),
                        "rejectionReason", Map.of(
                                "enum", Arrays.asList(
                                        "EDITORIAL_RULE_EXCLUDED",
                                        "NOT_PUBLISHABLE_CATEGORY",
                                        "SOURCE_RUN_QUOTA_EXCEEDED",
                                        "DUPLICATE_SEMANTIC_EVENT",
                                        "LOW_CONFIDENCE",
                                        "CLASSIFICATION_FAILED",
                                        null))),
                List.of(
                        "primaryCategoryCode",
                        "categoryCodes",
                        "semanticKey",
                        "semanticKeyAction",
                        "matchedSemanticEventId",
                        "confidence",
                        "shouldPublish",
                        "rejectionReason"));
    }

    private Map<String, Object> summaryResponseSchema() {
        return objectSchema(
                Map.of(
                        "title", stringSchema(),
                        "summary", stringSchema(),
                        "confidence", numberSchema()),
                List.of("title", "summary", "confidence"));
    }

    private Map<String, Object> translationResponseSchema() {
        return objectSchema(
                Map.of(
                        "title", stringSchema(),
                        "summary", stringSchema(),
                        "confidence", numberSchema()),
                List.of("title", "summary", "confidence"));
    }

    private Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        return Map.of(
                "type", "object",
                "properties", properties,
                "required", required,
                "additionalProperties", false);
    }

    private Map<String, Object> stringSchema() {
        return Map.of("type", "string");
    }

    private Map<String, Object> nullableStringSchema() {
        return Map.of("type", List.of("string", "null"));
    }

    private Map<String, Object> numberSchema() {
        return Map.of(
                "type", "number",
                "minimum", 0.0,
                "maximum", 1.0);
    }
}
