package com.example.limitservice.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.twelvedata")
public record TwelveDataProperties(
        String baseUrl,
        String apiKey,
        Duration connectTimeout,
        Duration readTimeout,
        int maxAttempts,
        Duration retryDelay
) {
}
