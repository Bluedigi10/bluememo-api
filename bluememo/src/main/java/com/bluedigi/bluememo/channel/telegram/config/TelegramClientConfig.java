package com.bluedigi.bluememo.channel.telegram.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class TelegramClientConfig {
    @Bean
    RestClient telegramRestClient(
            RestClient.Builder builder,
            TelegramProperties properties
    ) {
        return builder
                .baseUrl(properties.apiBaseUrl() + properties.botToken())
                .build();
    }
}
