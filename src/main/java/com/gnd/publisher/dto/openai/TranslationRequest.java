package com.gnd.publisher.dto.openai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TranslationRequest(
        @NotBlank String title,
        @NotBlank String summary,
        @NotBlank String sourceName,
        @NotBlank String sourceUrl,
        @NotBlank String sourceLanguage,
        @NotBlank String targetLanguage,
        @Positive int maxMessageCharacters) {
}
