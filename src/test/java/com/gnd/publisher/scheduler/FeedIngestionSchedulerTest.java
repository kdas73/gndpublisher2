package com.gnd.publisher.scheduler;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.gnd.publisher.config.SchedulerProperties;
import com.gnd.publisher.config.SchedulerProperties.ScheduledJob;
import com.gnd.publisher.logging.PipelineRunSupport;
import com.gnd.publisher.service.FeedIngestionService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FeedIngestionSchedulerTest {

    @Mock
    private FeedIngestionService feedIngestionService;

    private final PipelineRunSupport pipelineRunSupport = new PipelineRunSupport();

    @Test
    void callsIngestionServiceWhenIngestionJobIsEnabled() {
        FeedIngestionScheduler scheduler =
                new FeedIngestionScheduler(feedIngestionService, schedulerProperties(true), pipelineRunSupport);

        scheduler.ingestFeeds();

        verify(feedIngestionService).ingestEnabledSources();
    }

    @Test
    void doesNotCallIngestionServiceWhenIngestionJobIsDisabled() {
        FeedIngestionScheduler scheduler =
                new FeedIngestionScheduler(feedIngestionService, schedulerProperties(false), pipelineRunSupport);

        scheduler.ingestFeeds();

        verify(feedIngestionService, never()).ingestEnabledSources();
    }

    private SchedulerProperties schedulerProperties(boolean ingestionEnabled) {
        return new SchedulerProperties(
                new ScheduledJob(ingestionEnabled, "0 */3 * * * *"),
                new ScheduledJob(true, "0 */20 * * * *"),
                new ScheduledJob(true, "0 0 */6 * * *"),
                new ScheduledJob(true, "0 0 3 * * *"));
    }
}
