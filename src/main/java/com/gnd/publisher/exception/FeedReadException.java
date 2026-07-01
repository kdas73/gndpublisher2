package com.gnd.publisher.exception;

public class FeedReadException extends RuntimeException {

    public FeedReadException(String message, Throwable cause) {
        super(message, cause);
    }

    public FeedReadException(String message) {
        super(message);
    }
}
