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
    String environmentName) {

  public FlagsmithProperties {
    serverSideKey = Optional.of(serverSideKey).flatMap(o -> o);
    adminEmail = Optional.of(adminEmail).flatMap(o -> o);
    adminPassword = Optional.of(adminPassword).flatMap(o -> o);
    apiToken = Optional.of(apiToken).flatMap(o -> o);
  }
}
