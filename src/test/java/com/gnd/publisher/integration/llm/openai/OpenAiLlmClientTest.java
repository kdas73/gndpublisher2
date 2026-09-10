package com.gnd.publisher.integration.llm.openai;

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

class OpenAiLlmClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void postsToTheResponsesEndpointWithStrictJsonSchema() throws Exception {
        FakeLlmHttp.RecordingSender sender = new FakeLlmHttp.RecordingSender(
                responsesEnvelope("{\"semanticKey\":\"greek parliament approves migration bill\"}"));

        LlmCompletion completion = client(properties("https://api.openai.com/v1"), sender)
                .complete(completionRequest());

        assertThat(completion.outputText()).contains("semanticKey");
        assertThat(completion.model()).isEqualTo("gpt-5.4-mini");
        assertThat(completion.providerId()).isEqualTo("openai");

        assertThat(sender.lastRequest().uri())
                .hasToString("https://api.openai.com/v1/responses");
        assertThat(sender.lastRequest().timeout()).contains(Duration.ofSeconds(60));
        assertThat(sender.lastRequest().headers().firstValue("Authorization"))
                .contains("Bearer test-api-key");

        JsonNode body = objectMapper.readTree(FakeLlmHttp.requestBody(sender.lastRequest()));
        assertThat(body.path("model").asText()).isEqualTo("gpt-5.4-mini");
        assertThat(body.path("instructions").asText()).isEqualTo("Classify the item.");
        assertThat(body.path("store").asBoolean()).isFalse();

        JsonNode format = body.path("text").path("format");
        assertThat(format.path("type").asText()).isEqualTo("json_schema");
        assertThat(format.path("name").asText()).isEqualTo("category_classification_response");
        assertThat(format.path("strict").asBoolean()).isTrue();
        assertThat(format.path("schema").path("properties").has("primaryCategoryCode")).isTrue();

        // The input DTO travels as a JSON string, not as a nested object.
        assertThat(body.path("input").isTextual()).isTrue();
        assertThat(objectMapper.readTree(body.path("input").asText()).path("newsItem").path("sourceName").asText())
                .isEqualTo("ERT News");
    }

    @Test
    void honoursAConfiguredBaseUrl() {
        FakeLlmHttp.RecordingSender sender = new FakeLlmHttp.RecordingSender(
                responsesEnvelope("{\"semanticKey\":\"x\"}"));

        client(properties("https://openai.internal.example/v1/"), sender).complete(completionRequest());

        assertThat(sender.lastRequest().uri()).hasToString("https://openai.internal.example/v1/responses");
    }

    @Test
    void throwsForNonSuccessStatus() {
        FakeLlmHttp.RecordingSender sender = new FakeLlmHttp.RecordingSender(FakeLlmHttp.response(429, """
                {
                  "error": {
                    "message": "The model `missing-model` does not exist or you do not have access to it."
                  }
                }
                """));

        assertThatThrownBy(() -> client(properties("https://api.openai.com/v1"), sender).complete(completionRequest()))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("HTTP status 429")
                .hasMessageContaining("missing-model");
    }

    @Test
    void throwsForMissingOutputText() {
        FakeLlmHttp.RecordingSender sender = new FakeLlmHttp.RecordingSender(
                FakeLlmHttp.response(200, "{\"output\": []}"));

        assertThatThrownBy(() -> client(properties("https://api.openai.com/v1"), sender).complete(completionRequest()))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("output_text");
    }

    @Test
    void throwsWhenTheResponseIsIncomplete() {
        FakeLlmHttp.RecordingSender sender = new FakeLlmHttp.RecordingSender(FakeLlmHttp.response(200, """
                {"incomplete_details": {"reason": "max_output_tokens"}}
                """));

        assertThatThrownBy(() -> client(properties("https://api.openai.com/v1"), sender).complete(completionRequest()))
                .isInstanceOf(LlmIntegrationException.class)
                .hasMessageContaining("max_output_tokens");
    }

    @Test
    void doesNotLeakApiKeyOrRequestPayloadIntoExceptionMessageOnHttpError() {
        FakeLlmHttp.RecordingSender sender = new FakeLlmHttp.RecordingSender(FakeLlmHttp.response(429, """
                {
                  "error": {
                    "message": "The model `missing-model` does not exist or you do not have access to it."
                  }
                }
                """));

        assertThatThrownBy(() -> client(properties("https://api.openai.com/v1"), sender).complete(completionRequest()))
                .isInstanceOf(LlmIntegrationException.class)
                .extracting(Throwable::getMessage, InstanceOfAssertFactories.STRING)
                .doesNotContain("test-api-key")
                .doesNotContain("Bearer")
                .doesNotContain("Greek parliament approves new migration bill");
    }

    private OpenAiLlmClient client(LlmProperties.OpenAi properties, FakeLlmHttp.RecordingSender sender) {
        return new OpenAiLlmClient(properties, objectMapper, sender);
    }

    private LlmProperties.OpenAi properties(String baseUrl) {
        return new LlmProperties.OpenAi(
                "test-api-key",
                baseUrl,
                new LlmProperties.Timeouts(Duration.ofSeconds(5), Duration.ofSeconds(60)));
    }

    private LlmCompletionRequest completionRequest() {
        return new LlmCompletionRequest(
                "gpt-5.4-mini",
                "Classify the item.",
                """
                {"newsItem":{"title":"Greek parliament approves new migration bill","sourceName":"ERT News"}}""",
                "category_classification_response",
                LlmResponseSchemas.classificationResponseSchema(),
                Duration.ofSeconds(60));
    }

    private static java.net.http.HttpResponse<String> responsesEnvelope(String outputTextJson) {
        String escaped = outputTextJson
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        return FakeLlmHttp.response(200, """
                {
                  "id": "resp_test",
                  "error": null,
                  "incomplete_details": null,
                  "output": [
                    {
                      "type": "message",
                      "content": [
                        {
                          "type": "output_text",
                          "text": "%s"
                        }
                      ]
                    }
                  ]
                }
                """.formatted(escaped));
    }
}
