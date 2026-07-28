package com.gnd.publisher.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gnd.publisher.config.SchedulerProperties;
import com.gnd.publisher.config.SchedulerProperties.ScheduledJob;
import com.gnd.publisher.logging.PipelineRunSupport;
import com.gnd.publisher.service.CleanupService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CleanupSchedulerTest {

    @Mock
    private CleanupService cleanupService;

    private final PipelineRunSupport pipelineRunSupport = new PipelineRunSupport();

    @Test
    void callsCleanupServiceWhenCleanupJobIsEnabled() {
        CleanupScheduler scheduler = new CleanupScheduler(cleanupService, schedulerProperties(true), pipelineRunSupport);

        scheduler.runCleanup();

        verify(cleanupService).runCleanup();
    }

    @Test
    void doesNotCallCleanupServiceWhenCleanupJobIsDisabled() {
        CleanupScheduler scheduler = new CleanupScheduler(cleanupService, schedulerProperties(false), pipelineRunSupport);

        scheduler.runCleanup();

        verify(cleanupService, never()).runCleanup();
    }

    private SchedulerProperties schedulerProperties(boolean cleanupEnabled) {
        return new SchedulerProperties(
                new ScheduledJob(true, "0 */3 * * * *"),
                new ScheduledJob(true, "0 */20 * * * *"),
                new ScheduledJob(true, "0 0 */6 * * *"),
                new ScheduledJob(cleanupEnabled, "0 0 3 * * *"));
    }
}
