package com.bluedigi.bluememo.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "bluememo.link")
public record LinkProperties(
    @NotBlank
    String telegramUrl,
    @NotBlank
    String telegramBotName
) {
}
