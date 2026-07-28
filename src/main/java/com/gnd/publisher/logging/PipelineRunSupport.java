package com.gnd.publisher.logging;

import java.time.Clock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Outermost error boundary for scheduled pipeline runs. Assigns a run id (visible via
 * MDC to every log line emitted during the run) and ensures an unexpected failure is
 * logged with that context instead of falling through to Spring's default scheduled-task
 * error handler. This is a diagnosability backstop, not the primary isolation mechanism -
 * per-item/per-step boundaries inside each service remain the first line of defense.
 */
@Component
public class PipelineRunSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineRunSupport.class);

    private final Clock clock;

    public PipelineRunSupport() {
        this(Clock.systemUTC());
    }

    PipelineRunSupport(Clock clock) {
        this.clock = clock;
    }

    public void run(String stage, Runnable action) {
        String runId = RunIdGenerator.generate(stage, clock);
        try (LoggingContext.Scope ignored = LoggingContext.put(LogFields.RUN_ID, runId)) {
            LOGGER.info("Starting {} run {}", stage, runId);
            action.run();
            LOGGER.info("Completed {} run {}", stage, runId);
        } catch (Exception exception) {
            LOGGER.error("Pipeline stage '{}' run {} failed", stage, runId, exception);
        }
    }
}
