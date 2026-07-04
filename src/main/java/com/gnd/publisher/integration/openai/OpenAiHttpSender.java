package com.gnd.publisher.integration.openai;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public interface OpenAiHttpSender {

    HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException;
}
