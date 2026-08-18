package com.bluedigi.bluememo.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bluememo.jwt")
public record JwtProperties(
        String secret,
        long expirationMs
) {
}
