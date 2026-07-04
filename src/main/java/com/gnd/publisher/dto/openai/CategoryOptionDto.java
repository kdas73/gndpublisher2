package com.gnd.publisher.dto.openai;

import jakarta.validation.constraints.NotBlank;

public record CategoryOptionDto(
        @NotBlank String code,
        @NotBlank String description) {
}
