package com.gnd.publisher.logging;

import java.util.Map;

import org.slf4j.MDC;

/**
 * Thin try-with-resources helper around SLF4J's MDC so pipeline stages can attach
 * correlation ids (run id, source id, etc.) to their log lines without hand-rolling
 * push/pop boilerplate. Values are restored to their prior state on close, including
 * during exception unwind.
 */
public final class LoggingContext {

    private LoggingContext() {
    }

    public static Scope put(String key, String value) {
        if (value == null) {
            return () -> { };
        }
        MDC.MDCCloseable closeable = MDC.putCloseable(key, value);
        return closeable::close;
    }

    public static Scope put(Map<String, String> values) {
        Scope[] scopes = values.entrySet().stream()
                .map(entry -> put(entry.getKey(), entry.getValue()))
                .toArray(Scope[]::new);
        return () -> {
            for (int i = scopes.length - 1; i >= 0; i--) {
                scopes[i].close();
            }
        };
    }

    /** An {@link AutoCloseable} whose {@link #close()} never throws a checked exception. */
    @FunctionalInterface
    public interface Scope extends AutoCloseable {
        @Override
        void close();
    }
}
