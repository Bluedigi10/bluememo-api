package com.bluedigi.bluememo.channel.telegram.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class TelegramClientConfig {
    @Bean
    RestClient telegramRestClient(
            RestClient.Builder builder,
            TelegramProperties properties
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return builder
                .baseUrl(properties.apiBaseUrl() + properties.botToken())
                .requestFactory(requestFactory)
                .build();
    }
}
