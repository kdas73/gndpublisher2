package com.gnd.publisher.config;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gnd.categories")
public record CategoryProperties(
        @NotEmpty List<@Valid Category> options,
        @NotEmpty List<@NotBlank String> publishableCodes) {

    public record Category(
            @NotBlank String code,
            @NotBlank String name,
            @NotBlank String description,
            @Positive int publicationPriority,
            boolean enabled) {
    }
}
