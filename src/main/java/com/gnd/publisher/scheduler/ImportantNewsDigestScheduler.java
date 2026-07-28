package com.gnd.publisher.scheduler;

import com.gnd.publisher.config.SchedulerProperties;
import com.gnd.publisher.service.ImportantNewsDigestService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ImportantNewsDigestScheduler {

    private final ImportantNewsDigestService importantNewsDigestService;
    private final SchedulerProperties schedulerProperties;

    public ImportantNewsDigestScheduler(
            ImportantNewsDigestService importantNewsDigestService,
            SchedulerProperties schedulerProperties) {
        this.importantNewsDigestService = importantNewsDigestService;
        this.schedulerProperties = schedulerProperties;
    }

    @Scheduled(cron = "${gnd.scheduler.important-news-digest.cron}")
    public void publishImportantNewsDigest() {
        if (schedulerProperties.importantNewsDigest().enabled()) {
            importantNewsDigestService.publishImportantNewsDigest();
        }
    }
}
