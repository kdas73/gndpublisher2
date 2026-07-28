package com.gnd.publisher.scheduler;

import com.gnd.publisher.config.SchedulerProperties;
import com.gnd.publisher.logging.PipelineRunSupport;
import com.gnd.publisher.service.ImportantNewsDigestService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ImportantNewsDigestScheduler {

    private final ImportantNewsDigestService importantNewsDigestService;
    private final SchedulerProperties schedulerProperties;
    private final PipelineRunSupport pipelineRunSupport;

    public ImportantNewsDigestScheduler(
            ImportantNewsDigestService importantNewsDigestService,
            SchedulerProperties schedulerProperties,
            PipelineRunSupport pipelineRunSupport) {
        this.importantNewsDigestService = importantNewsDigestService;
        this.schedulerProperties = schedulerProperties;
        this.pipelineRunSupport = pipelineRunSupport;
    }

    @Scheduled(cron = "${gnd.scheduler.important-news-digest.cron}")
    public void publishImportantNewsDigest() {
        if (schedulerProperties.importantNewsDigest().enabled()) {
            pipelineRunSupport.run("digest", importantNewsDigestService::publishImportantNewsDigest);
        }
    }
}
