package com.gnd.publisher.integration.llm.ollama;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnd.publisher.config.LlmProperties;
import com.gnd.publisher.exception.LlmIntegrationException;
import com.gnd.publisher.integration.llm.FakeLlmHttp;
import com.gnd.publisher.integration.llm.LlmCompletion;
import com.gnd.publisher.integration.llm.LlmCompletionRequest;
import com.gnd.publisher.integration.llm.LlmResponseSchemas;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

class OllamaLlmClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void postsToTheChatEndpointWithTheSchemaAsFormat() throws Exception {
        FakeLlmHttp.RecordingSender sender = new FakeLlmHttp.RecordingSender(FakeLlmHttp.response(200, """
                {
                  "model": "translategemma:4b",
                  "message": {
                    "role": "assistant",
                    "content": "{\\"semanticKey\\":\\"greek parliament approves migration bill\\"}"
                  },
                  "done": true,
                  "done_reason": "stop"
                }
                """));

        LlmCompletion completion = client(properties("http://localhost:11434"), sender)
                .complete(completionRequest());

        assertThat(completion.outputText()).contains("semanticKey");
        assertThat(completion.model()).isEqualTo("translategemma:4b");
        assertThat(completion.providerId()).isEqualTo("ollama");

        assertThat(sender.lastRequest().uri()).hasToString("http://localhost:11434/api/chat");
        assertThat(sender.lastRequest().timeout()).contains(Duration.ofSeconds(300));

        // The Ollama API is unauthenticated; sending a bearer token would leak a secret.
        assertThat(sender.lastRequest().headers().firstValue("Authorization")).isEmpty();

        JsonNode body = objectMapper.readTree(FakeLlmHttp.requestBody(sender.lastRequest()));
        assertThat(body.path("model").asText()).isEqualTo("translategemma:4b");
        assertThat(body.path("stream").asBoolean()).isFalse();
    }

    @Test
    void sendsInstructionsAsSystemMessageAndInputAsUserMessage() throws Exception {
        FakeLlmHttp.RecordingSender sender = new FakeLlmHttp.RecordingSender(chatResponse("{\"a\":1}"));

        client(properties("http://localhost:11434"), sender).complete(completionRequest());

        JsonNode messages = objectMapper.readTree(FakeLlmHttp.requestBody(sender.lastRequest())).path("messages");
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).path("role").asText()).isEqualTo("system");
        assertThat(messages.get(0).path("content").asText()).isEqualTo("Classify the item.");
        assertThat(messages.get(1).path("role").asText()).isEqualTo("user");
        assertThat(messages.get(1).path("content").asText()).contains("ERT News");
    }

    @Test
    void sendsSchemaAsFormatAlongsideKeepAliveAndOptions() throws Exception {
        FakeLlmHttp.RecordingSender sender = new FakeLlmHttp.RecordingSender(chatResponse("{\"a\":1}"));

        client(properties("http://localhost:11434"), sender).complete(completionRequest());

        JsonNode body = objectMapper.readTree(FakeLlmHttp.requestBody(sender.lastRequest()));
        assertThat(body.path("stream").asBoolean()).isFalse();
        assertThat(body.path("keep_alive").asText()).isEqualTo("10m");
        assertThat(body.path("options").path("temperature").asDouble()).isZero();

        // format carries the JSON Schema itself, with no name or strict wrapper.
        assertThat(body.path("format"))
                .isEqualTo(objectMapper.valueToTree(LlmResponseSchemas.classificationResponseSchema()));
    }

    @Test
    void honoursAConfiguredBaseUrl() {
        FakeLlmHttp.RecordingSender sender = new FakeLlmHttp.RecordingSender(chatResponse("{\"a\":1}"));

        client(properties("http://ollama.internal.example:11434/"), sender).complete(completionRequest());

        assertThat(sender.lastRequest().uri()).hasToString("http://ollama.internal.example:11434/api/chat");
    }

    @Test
    void throwsWhenTheResponseCarriesAnErrorField() {
        // Ollama reports errors as a bare string, unlike OpenAI's error object.
        FakeLlmHttp.RecordingSender sender = new FakeLlmHttp.RecordingSender(FakeLlmHttp.response(200, """
                {"error": "model \\"translategemma:4b\\" not found, try pulling it first"}
                """));

        assertThatThrownBy(() -> client(properties("http://localhost:11434"), sender).complete(completionRequest()))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("not found, try pulling it first");
    }

    @Test
    void throwsForNonStopDoneReason() {
        FakeLlmHttp.RecordingSender sender = new FakeLlmHttp.RecordingSender(FakeLlmHttp.response(200, """
                {
                  "message": {"role": "assistant", "content": "{\\"semanticKey\\":\\"trunc"},
                  "done": true,
                  "done_reason": "length"
                }
                """));

        assertThatThrownBy(() -> client(properties("http://localhost:11434"), sender).complete(completionRequest()))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("done_reason=length");
    }

    @Test
    void throwsForMissingMessageContent() {
        FakeLlmHttp.RecordingSender sender = new FakeLlmHttp.RecordingSender(FakeLlmHttp.response(200, """
                {"message": {"role": "assistant", "content": ""}, "done": true, "done_reason": "stop"}
                """));

        assertThatThrownBy(() -> client(properties("http://localhost:11434"), sender).complete(completionRequest()))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("did not contain message content");
    }

    @Test
    void throwsForNonSuccessStatusUsingTheBareErrorString() {
        FakeLlmHttp.RecordingSender sender = new FakeLlmHttp.RecordingSender(FakeLlmHttp.response(404, """
                {"error": "model not found"}
                """));

        assertThatThrownBy(() -> client(properties("http://localhost:11434"), sender).complete(completionRequest()))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("HTTP status 404")
                .hasMessageContaining("model not found");
    }

    @Test
    void doesNotLeakRequestPayloadIntoExceptionMessageOnHttpError() {
        FakeLlmHttp.RecordingSender sender = new FakeLlmHttp.RecordingSender(FakeLlmHttp.response(500, """
                {"error": "internal server error"}
                """));

        assertThatThrownBy(() -> client(properties("http://localhost:11434"), sender).complete(completionRequest()))
                .isInstanceOf(LlmIntegrationException.class)
                .extracting(Throwable::getMessage, InstanceOfAssertFactories.STRING)
                .doesNotContain("Greek parliament approves new migration bill")
                .doesNotContain("Classify the item.");
    }

    private OllamaLlmClient client(LlmProperties.Ollama properties, FakeLlmHttp.RecordingSender sender) {
        return new OllamaLlmClient(properties, objectMapper, sender);
    }

    private LlmProperties.Ollama properties(String baseUrl) {
        return new LlmProperties.Ollama(
                baseUrl,
                new LlmProperties.Timeouts(Duration.ofSeconds(5), Duration.ofSeconds(300)),
                "10m",
                new LlmProperties.Options(0.0));
    }

    private LlmCompletionRequest completionRequest() {
        return new LlmCompletionRequest(
                "translategemma:4b",
                "Classify the item.",
                """
                {"newsItem":{"title":"Greek parliament approves new migration bill","sourceName":"ERT News"}}""",
                "category_classification_response",
                LlmResponseSchemas.classificationResponseSchema(),
                Duration.ofSeconds(300));
    }

    private static java.net.http.HttpResponse<String> chatResponse(String content) {
        String escaped = content.replace("\\", "\\\\").replace("\"", "\\\"");
        return FakeLlmHttp.response(200, """
                {
                  "model": "translategemma:4b",
                  "message": {"role": "assistant", "content": "%s"},
                  "done": true,
                  "done_reason": "stop"
                }
                """.formatted(escaped));
    }
}
