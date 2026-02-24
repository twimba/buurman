package com.buurman.config.models;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "flagsmith")
public record FlagsmithProperties(
    String apiUrl,
    boolean enableAnalytics,
    int environmentRefreshIntervalSeconds,
    Optional<String> serverSideKey,
    Optional<String> adminEmail,
    Optional<String> adminPassword,
    Optional<String> apiToken,
    String projectName,
    String environmentName) {}
