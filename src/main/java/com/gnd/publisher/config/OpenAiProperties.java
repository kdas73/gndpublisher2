package com.gnd.publisher.config;

import java.time.Duration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gnd.openai")
public record OpenAiProperties(
        @NotBlank String apiKey,
        @Valid @NotNull Models models,
        @Valid @NotNull Prompts prompts,
        @Valid @NotNull Timeouts timeouts,
        @NotNull Duration semanticEventLookupWindow) {

    @AssertTrue(message = "semanticEventLookupWindow must be positive")
    public boolean isPositiveSemanticEventLookupWindow() {
        return semanticEventLookupWindow != null
                && !semanticEventLookupWindow.isNegative()
                && !semanticEventLookupWindow.isZero();
    }

    public record Models(
            @NotBlank String categorization,
            @NotBlank String publicationContent) {
    }

    public record Prompts(
            @NotBlank String classificationVersion,
            @NotBlank String editorialRulesVersion,
            @NotBlank String publicationContentVersion) {
    }

    public record Timeouts(
            @NotNull Duration connect,
            @NotNull Duration read) {

        @AssertTrue(message = "connect timeout must be positive")
        public boolean isPositiveConnectTimeout() {
            return connect != null && !connect.isNegative() && !connect.isZero();
        }

        @AssertTrue(message = "read timeout must be positive")
        public boolean isPositiveReadTimeout() {
            return read != null && !read.isNegative() && !read.isZero();
        }
    }
}
