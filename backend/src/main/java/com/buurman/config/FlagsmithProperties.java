package com.buurman.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flagsmith")
public record FlagsmithProperties(
        String apiKey,
        String apiUrl,
        boolean enableAnalytics,
        int environmentRefreshIntervalSeconds,
        String adminEmail,
        String adminPassword,
        String projectName,
        String environmentName
) {}
