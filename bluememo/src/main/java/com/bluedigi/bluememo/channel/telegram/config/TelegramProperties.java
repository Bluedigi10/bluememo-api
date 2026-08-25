package com.bluedigi.bluememo.channel.telegram.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bluememo.telegram")
public record TelegramProperties(
        @NotBlank
        String webhookSecret,
        @NotBlank
        String botToken,
        @NotBlank
        String apiBaseUrl
) {
}
