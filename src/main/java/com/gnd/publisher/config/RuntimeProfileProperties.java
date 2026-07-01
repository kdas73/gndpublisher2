package com.gnd.publisher.config;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gnd")
public record RuntimeProfileProperties(
        @NotBlank String profile) {
}
