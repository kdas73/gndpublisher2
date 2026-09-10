package com.gnd.publisher.dto.llm;

import jakarta.validation.constraints.NotBlank;

public record SemanticEventKeyCandidateDto(
        @NotBlank String id,
        @NotBlank String semanticKey) {
}
