package com.gnd.publisher.integration.llm;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnd.publisher.exception.LlmIntegrationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Transport plumbing shared by every HTTP-based {@link LlmClient}: sending, status checking, and
 * failure mapping onto {@link LlmIntegrationException}.
 *
 * <p>{@link #complete(LlmCompletionRequest)} is final so that subclasses stay wire format only.
 * Exception messages are built solely from the response body, never from the request, so neither
 * credentials nor payloads can leak into logs.
 *
 * <p>Every call is logged: endpoint and model at DEBUG before sending, outcome (status, elapsed
 * time, output size) at INFO after, and a truncated request/response preview at DEBUG. Per
 * {@code docs/security.md}, request and response bodies may contain news text or generated
 * content, so previews are capped at {@link #MAX_ERROR_BODY_LENGTH} characters and gated behind
 * DEBUG rather than logged in full at INFO.
 */
public abstract class AbstractHttpLlmClient implements LlmClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractHttpLlmClient.class);

    protected static final int MAX_ERROR_BODY_LENGTH = 1000;

    protected final ObjectMapper objectMapper;

    private final LlmHttpSender httpSender;

    protected AbstractHttpLlmClient(ObjectMapper objectMapper, LlmHttpSender httpSender) {
        this.objectMapper = objectMapper;
        this.httpSender = httpSender;
    }

    @Override
    public final LlmCompletion complete(LlmCompletionRequest request) {
        HttpRequest httpRequest = httpRequest(request);
        LOGGER.debug("Calling {} model={} endpoint={}", providerId(), request.model(), httpRequest.uri());
        LOGGER.debug("{} request input preview: {}", providerId(), truncate(request.inputJson()));

        long startedAtNanos = System.nanoTime();
        HttpResponse<String> httpResponse = send(httpRequest);
        long elapsedMs = Duration.ofNanos(System.nanoTime() - startedAtNanos).toMillis();

        if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
            LOGGER.warn("{} call failed model={} status={} elapsedMs={}",
                    providerId(), request.model(), httpResponse.statusCode(), elapsedMs);
            throw new LlmIntegrationException(providerId()
                    + " returned HTTP status "
                    + httpResponse.statusCode()
                    + ": "
                    + errorDetails(httpResponse.body()));
        }

        String outputText = outputText(httpResponse.body());
        LOGGER.info("{} call succeeded model={} status={} elapsedMs={} outputChars={}",
                providerId(), request.model(), httpResponse.statusCode(), elapsedMs, outputText.length());
        LOGGER.debug("{} response output preview: {}", providerId(), truncate(outputText));

        return new LlmCompletion(outputText, request.model(), providerId());
    }

    /** Builds the provider-specific HTTP request, including endpoint, headers, and body. */
    protected abstract HttpRequest httpRequest(LlmCompletionRequest request);

    /** Extracts the structured output text from a successful provider response body. */
    protected abstract String outputText(String responseBody);

    /** Renders a provider error body as a short, secret-free diagnostic. */
    protected abstract String errorDetails(String responseBody);

    private HttpResponse<String> send(HttpRequest httpRequest) {
        try {
            return httpSender.send(httpRequest);
        } catch (IOException exception) {
            throw new LlmIntegrationException("Failed to call " + providerId(), exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LlmIntegrationException("Interrupted while calling " + providerId(), exception);
        }
    }

    protected String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new LlmIntegrationException("Failed to serialize " + providerId() + " payload", exception);
        }
    }

    protected JsonNode readTree(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            throw new LlmIntegrationException("Failed to parse " + providerId() + " response body", exception);
        }
    }

    protected String truncate(String value) {
        if (value.length() <= MAX_ERROR_BODY_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_BODY_LENGTH) + "...";
    }

    /** Joins a configured base URL and a fixed path, tolerating a trailing slash on the base URL. */
    protected static URI endpoint(String baseUrl, String path) {
        String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return URI.create(trimmed + path);
    }
}
