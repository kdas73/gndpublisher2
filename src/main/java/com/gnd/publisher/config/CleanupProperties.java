package com.gnd.publisher.config;

import java.time.Duration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gnd.cleanup")
public record CleanupProperties(
        @NotNull Duration newsItemsRetention,
        @NotNull Duration classificationRunsRetention,
        @NotNull Duration publicationsRetention,
        @NotNull Duration digestRetention) {

    @AssertTrue(message = "all retention periods must be positive")
    public boolean isPositiveRetentionPeriods() {
        return isPositive(newsItemsRetention)
                && isPositive(classificationRunsRetention)
                && isPositive(publicationsRetention)
                && isPositive(digestRetention);
    }

    private static boolean isPositive(Duration duration) {
        return duration != null && !duration.isNegative() && !duration.isZero();
    }
}
