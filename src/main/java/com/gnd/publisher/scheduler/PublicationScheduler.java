package com.gnd.publisher.scheduler;

import com.gnd.publisher.config.SchedulerProperties;
import com.gnd.publisher.logging.PipelineRunSupport;
import com.gnd.publisher.service.PublicationService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PublicationScheduler {

    private final PublicationService publicationService;
    private final SchedulerProperties schedulerProperties;
    private final PipelineRunSupport pipelineRunSupport;

    public PublicationScheduler(
            PublicationService publicationService,
            SchedulerProperties schedulerProperties,
            PipelineRunSupport pipelineRunSupport) {
        this.publicationService = publicationService;
        this.schedulerProperties = schedulerProperties;
        this.pipelineRunSupport = pipelineRunSupport;
    }

    @Scheduled(cron = "${gnd.scheduler.publication.cron}")
    public void publishSelectedContent() {
        if (schedulerProperties.publication().enabled()) {
            pipelineRunSupport.run("publication", publicationService::publishSelectedContent);
        }
    }
}
