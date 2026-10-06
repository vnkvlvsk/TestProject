package com.example.limitservice.config;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.limits")
public record LimitProperties(
        ZoneId zone,
        BigDecimal defaultLimitSum,
        Duration pendingRetryDelay
) {
}
