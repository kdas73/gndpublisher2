package com.gnd.publisher.config;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gnd.telegram")
public record TelegramProperties(
        @NotBlank String botToken,
        @NotEmpty List<@Valid Channel> channels) {

    public record Channel(
            @NotBlank String code,
            @NotBlank String language,
            @NotBlank String channelId,
            String username,
            @NotBlank String messageUrlTemplate,
            boolean enabled) {
    }
}
