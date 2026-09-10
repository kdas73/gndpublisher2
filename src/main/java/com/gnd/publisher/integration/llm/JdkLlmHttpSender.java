package com.gnd.publisher.integration.llm;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * JDK {@link HttpClient} transport. One instance per provider, because the connect timeout is
 * configured on the client rather than on each request. Built by {@code LlmClientsConfiguration}
 * instead of being component-scanned, since two instances of this class are required.
 */
public class JdkLlmHttpSender implements LlmHttpSender {

    private final HttpClient httpClient;

    public JdkLlmHttpSender(Duration connectTimeout) {
        this(HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build());
    }

    JdkLlmHttpSender(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
