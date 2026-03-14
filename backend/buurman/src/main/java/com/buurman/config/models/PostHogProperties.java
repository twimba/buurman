package com.buurman.config.models;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "posthog")
public record PostHogProperties(
    String host,
    String projectApiKey,
    Optional<String> personalApiKey,
    Optional<Long> projectId,
    int pollIntervalSeconds) {

  public PostHogProperties {
    personalApiKey = Optional.ofNullable(personalApiKey).flatMap(o -> o);
    projectId = Optional.ofNullable(projectId).flatMap(o -> o);
  }
}
