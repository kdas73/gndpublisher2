package com.gnd.publisher.exception;

public class TelegramPublishException extends RuntimeException {

    public TelegramPublishException(String message) {
        super(message);
    }

    public TelegramPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}
