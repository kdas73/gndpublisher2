package com.gnd.publisher.integration.telegram;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLSession;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnd.publisher.config.TelegramProperties;
import com.gnd.publisher.domain.model.TelegramChannel;
import com.gnd.publisher.dto.telegram.TelegramMessageDto;
import com.gnd.publisher.dto.telegram.TelegramSendResult;
import com.gnd.publisher.exception.TelegramPublishException;

import org.junit.jupiter.api.Test;

class HttpTelegramBotClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsMessageAndBuildsMessageUrl() throws Exception {
        FakeTelegramHttpSender sender = new FakeTelegramHttpSender(new FakeHttpResponse(200, """
                {
                  "ok": true,
                  "result": {
                    "message_id": 42
                  }
                }
                """));
        HttpTelegramBotClient client = client(sender);

        TelegramSendResult result = client.sendMessage(channel(), new TelegramMessageDto("-100123456", "Hello", "HTML"));

        assertThat(result.messageId()).isEqualTo("42");
        assertThat(result.messageUrl()).isEqualTo("https://t.me/gnd_news/42");
        assertThat(sender.lastRequest().uri()).isEqualTo(URI.create(
                "https://api.telegram.org/bottest-token/sendMessage"));
        JsonNode body = objectMapper.readTree(requestBody(sender.lastRequest()));
        assertThat(body.path("chat_id").asText()).isEqualTo("-100123456");
        assertThat(body.path("text").asText()).isEqualTo("Hello");
        assertThat(body.path("parse_mode").asText()).isEqualTo("HTML");
        assertThat(body.path("disable_web_page_preview").asBoolean()).isFalse();
    }

    @Test
    void throwsForTelegramErrorResponse() {
        FakeTelegramHttpSender sender = new FakeTelegramHttpSender(new FakeHttpResponse(200, """
                {
                  "ok": false,
                  "description": "Bad Request: chat not found"
                }
                """));
        HttpTelegramBotClient client = client(sender);

        assertThatThrownBy(() -> client.sendMessage(channel(), new TelegramMessageDto("-100123456", "Hello")))
                .isInstanceOf(TelegramPublishException.class)
                .hasMessageContaining("chat not found");
    }

    private HttpTelegramBotClient client(TelegramHttpSender sender) {
        return new HttpTelegramBotClient(
                new TelegramProperties(
                        "test-token",
                        List.of(new TelegramProperties.Channel(
                                "news-en",
                                "en",
                                "-100123456",
                                "gnd_news",
                                "https://t.me/{username}/{messageId}",
                                true))),
                objectMapper,
                sender);
    }

    private TelegramChannel channel() {
        return TelegramChannel.create(
                "news-en",
                "en",
                "-100123456",
                "gnd_news",
                "https://t.me/{username}/{messageId}",
                "News EN",
                true);
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

    private static final class FakeTelegramHttpSender implements TelegramHttpSender {

        private final HttpResponse<String> response;
        private HttpRequest lastRequest;

        private FakeTelegramHttpSender(HttpResponse<String> response) {
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
            return URI.create("https://api.telegram.org/bottest-token/sendMessage");
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
