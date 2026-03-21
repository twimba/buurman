package com.buurman.config.models;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import com.buurman.util.Generated;

@ConfigurationProperties(prefix = "flagsmith")
@Generated
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
    serverSideKey = Optional.ofNullable(serverSideKey).flatMap(o -> o);
    adminEmail = Optional.ofNullable(adminEmail).flatMap(o -> o);
    adminPassword = Optional.ofNullable(adminPassword).flatMap(o -> o);
    apiToken = Optional.ofNullable(apiToken).flatMap(o -> o);
  }
}
