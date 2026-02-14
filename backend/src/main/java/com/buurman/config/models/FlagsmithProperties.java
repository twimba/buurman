package com.buurman.config.models;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flagsmith")
public record FlagsmithProperties(
        String apiUrl,
        boolean enableAnalytics,
        int environmentRefreshIntervalSeconds,
        String adminEmail,
        String adminPassword,
        String projectName,
        String environmentName
) {}
