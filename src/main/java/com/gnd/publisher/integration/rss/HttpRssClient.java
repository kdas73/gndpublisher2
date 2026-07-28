package com.gnd.publisher.integration.rss;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.gnd.publisher.exception.FeedReadException;

import org.springframework.stereotype.Component;

@Component
public class HttpRssClient implements RssClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient;

    public HttpRssClient() {
        this(HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    HttpRssClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String fetch(URI feedUri) {
        HttpRequest request = HttpRequest.newBuilder(feedUri)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/rss+xml, application/xml, text/xml")
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new FeedReadException("RSS feed returned HTTP status " + response.statusCode());
            }
            return response.body();
        } catch (IOException exception) {
            throw new FeedReadException("Failed to read RSS feed " + feedUri, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new FeedReadException("Interrupted while reading RSS feed " + feedUri, exception);
        }
    }
}
