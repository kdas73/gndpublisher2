package com.gnd.publisher.config;

import java.time.Duration;
import java.util.Locale;

import com.gnd.publisher.exception.LlmIntegrationException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gnd.llm")
public record LlmProperties(
        @Valid @NotNull UseCase categorization,
        @Valid @NotNull UseCase publicationContent,
        @Valid @NotNull Prompts prompts,
        @Valid @NotNull OpenAi openai,
        @Valid @NotNull Ollama ollama) {

    private static final String OPENAI_PROVIDER_ID = "openai";
    private static final String OLLAMA_PROVIDER_ID = "ollama";

    /**
     * Read timeout for the given provider. The use-case layer knows only the provider id, while the
     * timeout is provider configuration, so the lookup lives here rather than in the adapters.
     */
    public Duration readTimeout(String providerId) {
        return switch (normalize(providerId)) {
            case OPENAI_PROVIDER_ID -> openai.timeouts().read();
            case OLLAMA_PROVIDER_ID -> ollama.timeouts().read();
            default -> throw new LlmIntegrationException("Unknown LLM provider id: " + providerId);
        };
    }

    @AssertTrue(message = "gnd.llm.openai.api-key is required when a use case targets the openai provider")
    public boolean isOpenAiApiKeyPresentWhenTargeted() {
        if (!targetsOpenAi(categorization) && !targetsOpenAi(publicationContent)) {
            return true;
        }
        return openai != null && openai.apiKey() != null && !openai.apiKey().isBlank();
    }

    private static boolean targetsOpenAi(UseCase useCase) {
        return useCase != null && OPENAI_PROVIDER_ID.equals(normalize(useCase.provider()));
    }

    private static String normalize(String providerId) {
        return providerId == null ? "" : providerId.trim().toLowerCase(Locale.ROOT);
    }

    public record UseCase(
            @NotBlank String provider,
            @NotBlank String model) {
    }

    public record Prompts(
            @NotBlank String classificationVersion,
            @NotBlank String editorialRulesVersion,
            @NotBlank String publicationContentVersion) {
    }

    /**
     * {@code apiKey} is deliberately not {@code @NotBlank}: it is required only when a use case
     * actually targets OpenAI. See {@link #isOpenAiApiKeyPresentWhenTargeted()}.
     */
    public record OpenAi(
            String apiKey,
            @NotBlank String baseUrl,
            @Valid @NotNull Timeouts timeouts) {
    }

    public record Ollama(
            @NotBlank String baseUrl,
            @Valid @NotNull Timeouts timeouts,
            @NotBlank String keepAlive,
            @Valid @NotNull Options options) {
    }

    public record Options(
            @NotNull @DecimalMin("0.0") @DecimalMax("2.0") Double temperature) {
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
