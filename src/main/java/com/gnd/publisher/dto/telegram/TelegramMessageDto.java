package com.gnd.publisher.dto.telegram;

public record TelegramMessageDto(
        String chatId,
        String text,
        String parseMode) {

    public TelegramMessageDto(String chatId, String text) {
        this(chatId, text, null);
    }
}
