package com.gnd.publisher.dto.telegram;

public record TelegramSendResult(
        String messageId,
        String messageUrl) {
}
