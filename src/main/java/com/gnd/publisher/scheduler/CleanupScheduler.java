package com.gnd.publisher.scheduler;

import com.gnd.publisher.config.SchedulerProperties;
import com.gnd.publisher.service.CleanupService;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CleanupScheduler {

    private final CleanupService cleanupService;
    private final SchedulerProperties schedulerProperties;

    public CleanupScheduler(
            CleanupService cleanupService,
            SchedulerProperties schedulerProperties) {
        this.cleanupService = cleanupService;
        this.schedulerProperties = schedulerProperties;
    }

    @Scheduled(cron = "${gnd.scheduler.cleanup.cron}")
    public void runCleanup() {
        if (schedulerProperties.cleanup().enabled()) {
            cleanupService.runCleanup();
        }
    }
}
