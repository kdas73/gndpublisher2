package com.gnd.publisher.scheduler;

import com.gnd.publisher.config.SchedulerProperties;
import com.gnd.publisher.logging.PipelineRunSupport;
import com.gnd.publisher.service.FeedIngestionService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class FeedIngestionScheduler {

    private final FeedIngestionService feedIngestionService;
    private final SchedulerProperties schedulerProperties;
    private final PipelineRunSupport pipelineRunSupport;

    public FeedIngestionScheduler(
            FeedIngestionService feedIngestionService,
            SchedulerProperties schedulerProperties,
            PipelineRunSupport pipelineRunSupport) {
        this.feedIngestionService = feedIngestionService;
        this.schedulerProperties = schedulerProperties;
        this.pipelineRunSupport = pipelineRunSupport;
    }

    @Scheduled(cron = "${gnd.scheduler.ingestion.cron}")
    public void ingestFeeds() {
        if (schedulerProperties.ingestion().enabled()) {
            pipelineRunSupport.run("ingestion", feedIngestionService::ingestEnabledSources);
        }
    }
}
