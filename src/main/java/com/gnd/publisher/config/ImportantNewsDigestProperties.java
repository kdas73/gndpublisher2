package com.gnd.publisher.config;

import java.time.Duration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gnd.important-news-digest")
public record ImportantNewsDigestProperties(
        boolean enabled,
        @NotBlank String cron,
        @Positive int duplicateThreshold,
        @NotBlank String targetLanguage,
        @NotBlank String channelCode,
        @NotNull Duration lookbackWindow) {

    @AssertTrue(message = "lookbackWindow must be positive")
    public boolean isPositiveLookbackWindow() {
        return lookbackWindow != null && !lookbackWindow.isNegative() && !lookbackWindow.isZero();
    }
}
