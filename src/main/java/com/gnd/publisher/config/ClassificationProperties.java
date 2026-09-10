package com.gnd.publisher.config;

import java.time.Duration;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gnd.classification")
public record ClassificationProperties(
        @NotNull Duration semanticEventLookupWindow) {

    @AssertTrue(message = "semanticEventLookupWindow must be positive")
    public boolean isPositiveSemanticEventLookupWindow() {
        return semanticEventLookupWindow != null
                && !semanticEventLookupWindow.isNegative()
                && !semanticEventLookupWindow.isZero();
    }
}
