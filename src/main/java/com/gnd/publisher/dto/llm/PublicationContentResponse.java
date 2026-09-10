package com.gnd.publisher.dto.llm;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public record PublicationContentResponse(
        @NotBlank String title,
        @NotBlank String summary,
        @DecimalMin("0.0") @DecimalMax("1.0") double confidence) {
}
