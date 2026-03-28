package com.buurman.config.models;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.buurman.util.SkipTestCoverage;

@ConfigurationProperties(prefix = "notification.outbox")
@SkipTestCoverage
public record NotificationOutboxProperties(int batchSize, int maxRetries) {}
