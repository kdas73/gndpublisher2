package com.gnd.publisher.scheduler;

import com.gnd.publisher.config.SchedulerProperties;
import com.gnd.publisher.service.PublicationService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PublicationScheduler {

    private final PublicationService publicationService;
    private final SchedulerProperties schedulerProperties;

    public PublicationScheduler(
            PublicationService publicationService,
            SchedulerProperties schedulerProperties) {
        this.publicationService = publicationService;
        this.schedulerProperties = schedulerProperties;
    }

    @Scheduled(cron = "${gnd.scheduler.publication.cron}")
    public void publishSelectedContent() {
        if (schedulerProperties.publication().enabled()) {
            publicationService.publishSelectedContent();
        }
    }
}
