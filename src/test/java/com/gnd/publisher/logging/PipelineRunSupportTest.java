package com.gnd.publisher.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

class PipelineRunSupportTest {

    private static final Instant NOW = Instant.parse("2026-07-01T12:00:00Z");

    private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
    private final PipelineRunSupport pipelineRunSupport = new PipelineRunSupport(Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void attachAppender() {
        appender.start();
        logger().addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        logger().detachAppender(appender);
        MDC.clear();
    }

    @Test
    void logsStartAndCompletionForSuccessfulRun() {
        AtomicBoolean ran = new AtomicBoolean(false);

        pipelineRunSupport.run("ingestion", () -> ran.set(true));

        assertThat(ran.get()).isTrue();
        assertThat(formattedMessages()).anyMatch(message -> message.startsWith("Starting ingestion run ingestion-"));
        assertThat(formattedMessages()).anyMatch(message -> message.startsWith("Completed ingestion run ingestion-"));
    }

    @Test
    void catchesAndLogsExceptionWithoutPropagating() {
        pipelineRunSupport.run("cleanup", () -> {
            throw new RuntimeException("boom");
        });

        assertThat(appender.list).anyMatch(event -> event.getLevel() == Level.ERROR
                && event.getFormattedMessage().contains("cleanup"));
    }

    @Test
    void clearsMdcAfterSuccessfulRun() {
        pipelineRunSupport.run("publication", () -> { });

        assertThat(MDC.get(LogFields.RUN_ID)).isNull();
    }

    @Test
    void clearsMdcAfterFailedRun() {
        pipelineRunSupport.run("digest", () -> {
            throw new RuntimeException("boom");
        });

        assertThat(MDC.get(LogFields.RUN_ID)).isNull();
    }

    private static Logger logger() {
        return (Logger) LoggerFactory.getLogger(PipelineRunSupport.class);
    }

    private List<String> formattedMessages() {
        return appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }
}
