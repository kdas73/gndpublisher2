package com.gnd.publisher.dto.openai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record SummaryRequest(
        @NotBlank String title,
        String summary,
        @NotBlank String sourceName,
        @NotBlank String sourceUrl,
        @NotBlank String targetLanguage,
        @Positive int maxCharacters) {
}
