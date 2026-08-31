package com.bluedigi.bluememo.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bluememo.jwt")
public record JwtProperties(
        String secret,
        Duration expirationMs
) {
}
