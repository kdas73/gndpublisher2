package com.gnd.publisher.integration.llm;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLSession;

/**
 * Hand-written HTTP doubles shared by the provider adapter tests. Public and in the parent package
 * so that both {@code integration.llm.openai} and {@code integration.llm.ollama} tests can use them
 * without pulling in an HTTP mocking dependency.
 */
public final class FakeLlmHttp {

    private FakeLlmHttp() {
    }

    public static HttpResponse<String> response(int statusCode, String body) {
        return new FakeHttpResponse(statusCode, body);
    }

    /** Drains a request body publisher synchronously so tests can assert on the serialized body. */
    public static String requestBody(HttpRequest request) {
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

    /** Returns a canned response and records the request that was sent. */
    public static final class RecordingSender implements LlmHttpSender {

        private final HttpResponse<String> response;
        private HttpRequest lastRequest;

        public RecordingSender(HttpResponse<String> response) {
            this.response = response;
        }

        @Override
        public HttpResponse<String> send(HttpRequest request) {
            this.lastRequest = request;
            return response;
        }

        public HttpRequest lastRequest() {
            return lastRequest;
        }
    }

    private record FakeHttpResponse(int statusCode, String body) implements HttpResponse<String> {

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
            return HttpHeaders.of(Map.of(), (name, value) -> true);
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return URI.create("http://localhost/");
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
