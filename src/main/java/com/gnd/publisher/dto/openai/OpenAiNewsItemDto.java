package com.gnd.publisher.dto.openai;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OpenAiNewsItemDto(
        @NotBlank String title,
        String summary,
        @NotBlank String sourceName,
        @NotBlank String sourceUrl,
        @NotNull Instant publishedAt) {
}
