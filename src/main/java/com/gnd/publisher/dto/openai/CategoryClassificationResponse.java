package com.gnd.publisher.dto.openai;

import java.util.List;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CategoryClassificationResponse(
        @NotBlank String primaryCategoryCode,
        @NotEmpty List<String> categoryCodes,
        @NotBlank String semanticKey,
        @NotNull SemanticKeyActionDto semanticKeyAction,
        String matchedSemanticEventId,
        @DecimalMin("0.0") @DecimalMax("1.0") double confidence,
        boolean shouldPublish,
        ClassificationRejectionReasonDto rejectionReason) {
}
