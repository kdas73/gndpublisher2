package com.gnd.publisher.dto.llm;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

public record PublicationContentRequest(
        @NotBlank String title,
        String summary,
        @NotBlank String sourceName,
        @NotBlank String sourceUrl,
        @NotBlank String sourceLanguage,
        @NotBlank String targetLanguage,
        @NotBlank String semanticKey,
        @NotBlank String primaryCategoryCode,
        @NotEmpty List<String> categoryCodes,
        @Positive int maxSummaryCharacters,
        @Positive int maxMessageCharacters) {
}
