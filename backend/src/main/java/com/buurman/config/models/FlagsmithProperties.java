package com.buurman.config.models;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flagsmith")
public record FlagsmithProperties(
    String apiUrl,
    boolean enableAnalytics,
    int environmentRefreshIntervalSeconds,
    @Nullable String serverSideKey,
    @Nullable String adminEmail,
    @Nullable String adminPassword,
    @Nullable String apiToken,
    String projectName,
    String environmentName) {}
