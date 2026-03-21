package com.buurman.config.models;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.buurman.util.Generated;

@ConfigurationProperties(prefix = "notification.outbox")
@Generated
public record NotificationOutboxProperties(int batchSize, int maxRetries) {}
