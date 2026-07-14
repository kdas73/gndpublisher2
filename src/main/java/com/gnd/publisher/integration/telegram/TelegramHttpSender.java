package com.gnd.publisher.integration.telegram;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public interface TelegramHttpSender {

    HttpResponse<String> send(HttpRequest request) throws IOException, InterruptedException;
}
