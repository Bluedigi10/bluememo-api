package com.bluedigi.bluememo.config.properties;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bluememo.cors")
public record CorsProperties (
    List<String> allowedOrigins
){
}
