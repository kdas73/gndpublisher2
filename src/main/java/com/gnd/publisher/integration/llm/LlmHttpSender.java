package com.gnd.publisher.integration.llm;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public interface LlmHttpSender {

    HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException;
}
