package com.gnd.publisher.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gnd.scheduler")
public record SchedulerProperties(
        @Valid @NotNull ScheduledJob ingestion,
        @Valid @NotNull ScheduledJob publication,
        @Valid @NotNull ScheduledJob importantNewsDigest,
        @Valid @NotNull ScheduledJob cleanup) {

    public record ScheduledJob(
            boolean enabled,
            @NotBlank String cron) {
    }
}
