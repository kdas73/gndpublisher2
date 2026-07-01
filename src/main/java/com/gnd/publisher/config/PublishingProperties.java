package com.gnd.publisher.config;

import java.util.Map;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gnd.publishing")
public record PublishingProperties(
        @NotEmpty Set<@NotBlank String> targetLanguages,
        @Positive int defaultMaxItemsPerSourcePerRun,
        Map<@NotBlank String, @Positive Integer> sourceQuotaOverrides,
        @Positive int summaryMaxCharacters,
        @Positive int telegramMaxMessageCharacters,
        @Valid @NotNull ImportantNewsDigest digest) {

    @AssertTrue(message = "telegramMaxMessageCharacters must be greater than or equal to summaryMaxCharacters")
    public boolean isMessageLimitAtLeastSummaryLimit() {
        return telegramMaxMessageCharacters >= summaryMaxCharacters;
    }

    public record ImportantNewsDigest(
            boolean enabled,
            @Positive int duplicateThreshold,
            @NotBlank String channelRoutingKey) {
    }
}
