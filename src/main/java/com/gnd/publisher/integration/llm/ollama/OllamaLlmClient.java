package com.gnd.publisher.integration.llm.ollama;

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
 * Native Ollama {@code /api/chat} wire format. Structured output is requested by passing the JSON
 * Schema as {@code format}; the schema name is not part of this wire format and is ignored.
 */
public class OllamaLlmClient extends AbstractHttpLlmClient {

    public static final String PROVIDER_ID = "ollama";

    private static final String COMPLETED_DONE_REASON = "stop";

    private final LlmProperties.Ollama properties;
    private final URI chatUri;

    public OllamaLlmClient(
            LlmProperties.Ollama properties,
            ObjectMapper objectMapper,
            LlmHttpSender httpSender) {
        super(objectMapper, httpSender);
        this.properties = properties;
        this.chatUri = endpoint(properties.baseUrl(), "/api/chat");
    }

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    protected HttpRequest httpRequest(LlmCompletionRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", request.model());
        body.put("messages", List.of(
                Map.of("role", "system", "content", request.instructions()),
                Map.of("role", "user", "content", request.inputJson())));
        body.put("stream", false);
        body.put("format", request.jsonSchema());
        body.put("keep_alive", properties.keepAlive());
        body.put("options", Map.of("temperature", properties.options().temperature()));

        return HttpRequest.newBuilder(chatUri)
                .timeout(request.readTimeout())
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
            throw new LlmIntegrationException("Ollama response contained an error: "
                    + truncate(error.asText("unknown error")));
        }

        // An absent done_reason is accepted; "length" means the output was truncated and is
        // therefore invalid JSON, which is clearer to report here than as a parse failure later.
        String doneReason = root.path("done_reason").asText("");
        if (!doneReason.isBlank() && !COMPLETED_DONE_REASON.equals(doneReason)) {
            throw new LlmIntegrationException("Ollama response did not complete: done_reason=" + doneReason);
        }

        String content = root.path("message").path("content").asText("").strip();
        if (content.isBlank()) {
            throw new LlmIntegrationException("Ollama response did not contain message content");
        }
        return content;
    }

    @Override
    protected String errorDetails(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "empty response body";
        }
        try {
            // Ollama reports errors as a bare string, not as an object with a message field.
            String message = objectMapper.readTree(responseBody).path("error").asText();
            if (!message.isBlank()) {
                return truncate(message);
            }
        } catch (JsonProcessingException ignored) {
            // Fall back to a truncated raw body when Ollama returns non-JSON diagnostics.
        }
        return truncate(responseBody);
    }
}
