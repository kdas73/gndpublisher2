package com.gnd.publisher.integration.openai;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.gnd.publisher.config.OpenAiProperties;

import org.springframework.stereotype.Component;

@Component
public class JdkOpenAiHttpSender implements OpenAiHttpSender {

    private final HttpClient httpClient;

    public JdkOpenAiHttpSender(OpenAiProperties properties) {
        this(HttpClient.newBuilder()
                .connectTimeout(properties.timeouts().connect())
                .build());
    }

    JdkOpenAiHttpSender(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
