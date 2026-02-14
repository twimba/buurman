package com.buurman.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flagsmith")
public record FlagsmithProperties(
        String apiKey,
        String apiKeyFile,
        String apiUrl,
        boolean enableAnalytics,
        int environmentRefreshIntervalSeconds
) {}
