package com.gnd.publisher.integration.telegram;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gnd.publisher.config.TelegramProperties;
import com.gnd.publisher.domain.model.TelegramChannel;
import com.gnd.publisher.dto.telegram.TelegramMessageDto;
import com.gnd.publisher.dto.telegram.TelegramSendResult;
import com.gnd.publisher.exception.TelegramPublishException;

import org.springframework.stereotype.Component;

@Component
public class HttpTelegramBotClient implements TelegramBotClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final int MAX_ERROR_BODY_LENGTH = 1000;

    private final TelegramProperties properties;
    private final ObjectMapper objectMapper;
    private final TelegramHttpSender httpSender;

    public HttpTelegramBotClient(
            TelegramProperties properties,
            ObjectMapper objectMapper,
            TelegramHttpSender httpSender) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpSender = httpSender;
    }

    @Override
    public TelegramSendResult sendMessage(TelegramChannel channel, TelegramMessageDto message) {
        HttpRequest request = HttpRequest.newBuilder(sendMessageUri())
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody(message)))
                .build();

        HttpResponse<String> response = send(request);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new TelegramPublishException("Telegram returned HTTP status "
                    + response.statusCode()
                    + ": "
                    + errorDetails(response.body()));
        }

        String messageId = messageId(response.body());
        return new TelegramSendResult(messageId, messageUrl(channel, messageId));
    }

    private URI sendMessageUri() {
        return URI.create("https://api.telegram.org/bot"
                + properties.botToken()
                + "/sendMessage");
    }

    private String requestBody(TelegramMessageDto message) {
        try {
            Map<String, Object> payload = new java.util.LinkedHashMap<>();
            payload.put("chat_id", message.chatId());
            payload.put("text", message.text());
            if (message.parseMode() != null && !message.parseMode().isBlank()) {
                payload.put("parse_mode", message.parseMode());
            }
            payload.put("disable_web_page_preview", false);
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new TelegramPublishException("Failed to serialize Telegram payload", exception);
        }
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpSender.send(request);
        } catch (IOException exception) {
            throw new TelegramPublishException("Failed to call Telegram", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new TelegramPublishException("Interrupted while calling Telegram", exception);
        }
    }

    private String messageId(String responseBody) {
        JsonNode root = parseResponseBody(responseBody);
        if (!root.path("ok").asBoolean(false)) {
            throw new TelegramPublishException("Telegram response was not ok: " + errorDetails(responseBody));
        }
        JsonNode messageId = root.path("result").path("message_id");
        if (messageId.isMissingNode() || messageId.isNull()) {
            throw new TelegramPublishException("Telegram response did not contain result.message_id");
        }
        return messageId.asText();
    }

    private JsonNode parseResponseBody(String responseBody) {
        try {
            return objectMapper.readTree(responseBody);
        } catch (JsonProcessingException exception) {
            throw new TelegramPublishException("Failed to parse Telegram response body", exception);
        }
    }

    private String messageUrl(TelegramChannel channel, String messageId) {
        return channel.getMessageUrlTemplate()
                .replace("{username}", channel.getUsername() == null ? "" : channel.getUsername())
                .replace("{channelId}", channel.getChannelId())
                .replace("{messageId}", messageId);
    }

    private String errorDetails(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "empty response body";
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String description = root.path("description").asText();
            if (!description.isBlank()) {
                return truncate(description);
            }
        } catch (JsonProcessingException ignored) {
            // Fall back to a truncated raw body when Telegram returns non-JSON diagnostics.
        }
        return truncate(responseBody);
    }

    private String truncate(String value) {
        if (value.length() <= MAX_ERROR_BODY_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_ERROR_BODY_LENGTH) + "...";
    }
}
