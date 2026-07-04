package com.gnd.publisher.integration.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.gnd.publisher.config.OpenAiProperties;
import com.gnd.publisher.dto.openai.CategoryClassificationRequest;
import com.gnd.publisher.dto.openai.CategoryClassificationResponse;
import com.gnd.publisher.dto.openai.CategoryOptionDto;
import com.gnd.publisher.dto.openai.OpenAiNewsItemDto;
import com.gnd.publisher.dto.openai.SemanticEventKeyCandidateDto;
import com.gnd.publisher.dto.openai.SummaryRequest;
import com.gnd.publisher.dto.openai.SummaryResponse;
import com.gnd.publisher.dto.openai.TranslationRequest;
import com.gnd.publisher.dto.openai.TranslationResponse;
import com.gnd.publisher.exception.OpenAiIntegrationException;

import org.junit.jupiter.api.Test;

class HttpOpenAiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final PromptLoader promptLoader = new PromptLoader();
    private final OpenAiResponseValidator validator = new OpenAiResponseValidator();

    @Test
    void sendsClassificationRequestAndParsesOutputText() throws Exception {
        FakeOpenAiHttpSender sender = new FakeOpenAiHttpSender(openAiResponse("""
                {
                  "primaryCategoryCode": "politics",
                  "categoryCodes": ["politics"],
                  "semanticKey": "greek parliament approves new migration bill",
                  "semanticKeyAction": "matched_existing",
                  "matchedSemanticEventId": "event-456",
                  "confidence": 0.91,
                  "shouldPublish": true,
                  "rejectionReason": null
                }
                """));
        HttpOpenAiClient client = client(sender);

        CategoryClassificationResponse response = client.classify(classificationRequest());

        assertThat(response.semanticKey()).isEqualTo("greek parliament approves new migration bill");
        HttpRequest request = sender.lastRequest();
        assertThat(request.uri()).isEqualTo(URI.create("https://api.openai.com/v1/responses"));
        assertThat(request.headers().firstValue("Authorization")).contains("Bearer test-api-key");

        JsonNode body = objectMapper.readTree(requestBody(request));
        assertThat(body.path("model").asText()).isEqualTo("GPT5.5-mini");
        assertThat(body.path("instructions").asText()).contains("classify Greek news items");
        assertThat(body.path("text").path("format").path("type").asText()).isEqualTo("json_schema");
        assertThat(body.path("store").asBoolean()).isFalse();

        JsonNode input = objectMapper.readTree(body.path("input").asText());
        assertThat(input.path("newsItem").path("sourceName").asText()).isEqualTo("ERT News");
        assertThat(input.path("candidateSemanticEvents").get(0).path("id").asText()).isEqualTo("event-456");
    }

    @Test
    void sendsSummaryRequestAndParsesOutputText() {
        FakeOpenAiHttpSender sender = new FakeOpenAiHttpSender(openAiResponse("""
                {
                  "title": "Greek parliament approves new migration bill",
                  "summary": "Greek lawmakers approved a new migration bill after debate.",
                  "confidence": 0.9
                }
                """));
        HttpOpenAiClient client = client(sender);

        SummaryResponse response = client.summarize(summaryRequest());

        assertThat(response.summary()).contains("migration bill");
        assertThat(sender.lastRequest()).satisfies(request ->
                assertThat(requestBody(request)).contains("publication summaries", "GPT-5.5"));
    }

    @Test
    void sendsTranslationRequestAndParsesOutputText() {
        FakeOpenAiHttpSender sender = new FakeOpenAiHttpSender(openAiResponse("""
                {
                  "title": "Greek parliament approves new migration bill",
                  "summary": "Greek lawmakers approved a new migration bill after debate.",
                  "confidence": 0.92
                }
                """));
        HttpOpenAiClient client = client(sender);

        TranslationResponse response = client.translate(translationRequest());

        assertThat(response.summary()).contains("migration bill");
        assertThat(requestBody(sender.lastRequest())).contains("translate publishable news", "maxMessageCharacters");
    }

    @Test
    void throwsForNonSuccessStatus() {
        FakeOpenAiHttpSender sender = new FakeOpenAiHttpSender(new FakeHttpResponse(429, "{}"));
        HttpOpenAiClient client = client(sender);

        assertThatThrownBy(() -> client.summarize(summaryRequest()))
                .isInstanceOf(OpenAiIntegrationException.class)
                .hasMessageContaining("HTTP status 429");
    }

    @Test
    void throwsForMissingOutputText() {
        FakeOpenAiHttpSender sender = new FakeOpenAiHttpSender(new FakeHttpResponse(200, "{\"output\": []}"));
        HttpOpenAiClient client = client(sender);

        assertThatThrownBy(() -> client.summarize(summaryRequest()))
                .isInstanceOf(OpenAiIntegrationException.class)
                .hasMessageContaining("output_text");
    }

    @Test
    void throwsForInvalidClassificationResponse() {
        FakeOpenAiHttpSender sender = new FakeOpenAiHttpSender(openAiResponse("""
                {
                  "primaryCategoryCode": "sports",
                  "categoryCodes": ["sports"],
                  "semanticKey": "some event",
                  "semanticKeyAction": "created_new",
                  "matchedSemanticEventId": null,
                  "confidence": 0.5,
                  "shouldPublish": false,
                  "rejectionReason": "NOT_PUBLISHABLE_CATEGORY"
                }
                """));
        HttpOpenAiClient client = client(sender);

        assertThatThrownBy(() -> client.classify(classificationRequest()))
                .isInstanceOf(OpenAiIntegrationException.class)
                .hasMessageContaining("not configured");
    }

    private HttpOpenAiClient client(OpenAiHttpSender sender) {
        return new HttpOpenAiClient(properties(), promptLoader, objectMapper, sender, validator);
    }

    private OpenAiProperties properties() {
        return new OpenAiProperties(
                "test-api-key",
                new OpenAiProperties.Models("GPT5.5-mini", "GPT-5.5", "GPT-5.5"),
                new OpenAiProperties.Prompts(
                        "classification-v1",
                        "editorial-rules-v1",
                        "summary-v1",
                        "translation-v1"),
                new OpenAiProperties.Timeouts(Duration.ofSeconds(5), Duration.ofSeconds(60)),
                Duration.ofDays(3));
    }

    private CategoryClassificationRequest classificationRequest() {
        return new CategoryClassificationRequest(
                new OpenAiNewsItemDto(
                        "Greek parliament approves new migration bill",
                        "Greek lawmakers approved a new migration bill after a lengthy debate.",
                        "ERT News",
                        "https://example.gr/news/item",
                        Instant.parse("2026-07-01T10:15:00Z")),
                List.of(
                        new CategoryOptionDto(
                                "politics",
                                "Government, elections, laws, public administration"),
                        new CategoryOptionDto(
                                "weather",
                                "Weather alerts, climate events, natural hazards")),
                List.of("politics"),
                List.of("For sports news, publish only items with concrete match results."),
                List.of(new SemanticEventKeyCandidateDto(
                        "event-456",
                        "greek parliament debates migration bill")));
    }

    private SummaryRequest summaryRequest() {
        return new SummaryRequest(
                "Greek parliament approves new migration bill",
                "Greek lawmakers approved a new migration bill after a lengthy debate.",
                "ERT News",
                "https://example.gr/news/item",
                "en",
                600);
    }

    private TranslationRequest translationRequest() {
        return new TranslationRequest(
                "Greek parliament approves new migration bill",
                "Greek lawmakers approved a new migration bill after a lengthy parliamentary debate.",
                "ERT News",
                "https://example.gr/news/item",
                "el",
                "en",
                3500);
    }

    private static FakeHttpResponse openAiResponse(String outputTextJson) {
        String escapedOutputText = outputTextJson
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n");
        return new FakeHttpResponse(200, """
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
                """.formatted(escapedOutputText));
    }

    private static String requestBody(HttpRequest request) {
        AtomicReference<String> body = new AtomicReference<>("");
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<>() {
            private final StringBuilder builder = new StringBuilder();

            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(ByteBuffer item) {
                builder.append(StandardCharsets.UTF_8.decode(item));
            }

            @Override
            public void onError(Throwable throwable) {
                throw new AssertionError("Failed to read request body", throwable);
            }

            @Override
            public void onComplete() {
                body.set(builder.toString());
            }
        });
        return body.get();
    }

    private static final class FakeOpenAiHttpSender implements OpenAiHttpSender {

        private final HttpResponse<String> response;
        private HttpRequest lastRequest;

        private FakeOpenAiHttpSender(HttpResponse<String> response) {
            this.response = response;
        }

        @Override
        public HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
            lastRequest = request;
            return response;
        }

        private HttpRequest lastRequest() {
            return lastRequest;
        }
    }

    private static final class FakeHttpResponse implements HttpResponse<String> {

        private final int statusCode;
        private final String body;

        private FakeHttpResponse(int statusCode, String body) {
            this.statusCode = statusCode;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (first, second) -> true);
        }

        @Override
        public String body() {
            return body;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return URI.create("https://api.openai.com/v1/responses");
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
