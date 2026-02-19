package com.buurman.config.models;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flagsmith")
public record FlagsmithProperties(
    String apiUrl,
    boolean enableAnalytics,
    int environmentRefreshIntervalSeconds,
    String serverSideKey,
    String adminEmail,
    String adminPassword,
    String apiToken,
    String projectName,
    String environmentName) {}
