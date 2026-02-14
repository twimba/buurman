package com.buurman.config.models;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "notification.outbox")
public record NotificationOutboxProperties(
        long pollIntervalMs,
        int batchSize,
        int maxRetries
) {}
