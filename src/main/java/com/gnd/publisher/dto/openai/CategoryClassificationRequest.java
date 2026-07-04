package com.gnd.publisher.dto.openai;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CategoryClassificationRequest(
        @Valid @NotNull OpenAiNewsItemDto newsItem,
        @Valid @NotEmpty List<CategoryOptionDto> categoryOptions,
        @NotEmpty List<String> publishableCategoryCodes,
        @NotNull List<String> editorialRules,
        @Valid @NotNull List<SemanticEventKeyCandidateDto> candidateSemanticEvents) {
}
