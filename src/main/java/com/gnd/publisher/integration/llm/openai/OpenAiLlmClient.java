package com.gnd.publisher.integration.llm.openai;

import java.net.URI;
import java.net.http.HttpRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnd.publisher.config.LlmProperties;
import com.gnd.publisher.exception.LlmIntegrationException;
import com.gnd.publisher.integration.llm.AbstractHttpLlmClient;
import com.gnd.publisher.integration.llm.LlmCompletionRequest;
import com.gnd.publisher.integration.llm.LlmHttpSender;

/**
 * OpenAI Responses API wire format. Structured output is requested through
 * {@code text.format} with {@code strict} JSON Schema.
 */
public class OpenAiLlmClient extends AbstractHttpLlmClient {

    public static final String PROVIDER_ID = "openai";

    private static final String OUTPUT_TEXT_TYPE = "output_text";

    private final LlmProperties.OpenAi properties;
    private final URI responsesUri;

    public OpenAiLlmClient(
            LlmProperties.OpenAi properties,
            ObjectMapper objectMapper,
            LlmHttpSender httpSender) {
        super(objectMapper, httpSender);
        this.properties = properties;
        this.responsesUri = endpoint(properties.baseUrl(), "/responses");
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    protected HttpRequest httpRequest(LlmCompletionRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.model());
        body.put("instructions", request.instructions());
        body.put("input", request.inputJson());
        body.put("text", Map.of(
                "format", Map.of(
                        "type", "json_schema",
                        "name", request.schemaName(),
                        "strict", true,
                        "schema", request.jsonSchema())));
        body.put("tools", List.of());
        body.put("store", false);

        return HttpRequest.newBuilder(responsesUri)
                .timeout(request.readTimeout())
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(body)))
                .build();
    }

    @Override
    protected String outputText(String responseBody) {
        JsonNode root = readTree(responseBody);
        JsonNode error = root.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            throw new LlmIntegrationException("OpenAI response contained an error: "
                    + error.path("message").asText("unknown error"));
        }
        JsonNode incompleteDetails = root.path("incomplete_details");
        if (!incompleteDetails.isMissingNode() && !incompleteDetails.isNull()) {
            throw new LlmIntegrationException("OpenAI response was incomplete: "
                    + incompleteDetails.path("reason").asText("unknown reason"));
        }

        JsonNode output = root.path("output");
        if (!output.isArray()) {
            throw new LlmIntegrationException("OpenAI response did not contain output array");
        }
        for (JsonNode outputItem : output) {
            JsonNode content = outputItem.path("content");
            if (!content.isArray()) {
                continue;
            }
            for (JsonNode contentItem : content) {
                if (OUTPUT_TEXT_TYPE.equals(contentItem.path("type").asText())) {
                    String text = contentItem.path("text").asText();
                    if (!text.isBlank()) {
                        return text;
                    }
                }
            }
        }
        throw new LlmIntegrationException("OpenAI response did not contain output_text");
    }

    @Override
    protected String errorDetails(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "empty response body";
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String message = root.path("error").path("message").asText();
            if (!message.isBlank()) {
                return truncate(message);
            }
        } catch (JsonProcessingException ignored) {
            // Fall back to a truncated raw body when OpenAI returns non-JSON diagnostics.
        }
        return truncate(responseBody);
    }
}
