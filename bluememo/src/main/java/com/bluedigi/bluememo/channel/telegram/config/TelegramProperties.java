package com.bluedigi.bluememo.channel.telegram.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bluememo.telegram")
public record TelegramProperties(
        String webhookSecret,
        String botToken,
        String apiBaseUrl
) {
}
