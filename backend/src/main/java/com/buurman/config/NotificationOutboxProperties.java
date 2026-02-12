package com.buurman.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.outbox")
public record NotificationOutboxProperties(
        long pollIntervalMs,
        int batchSize,
        int maxRetries
) {}
