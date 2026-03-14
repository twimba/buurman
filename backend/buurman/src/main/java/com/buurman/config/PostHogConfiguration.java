package com.buurman.config;

import java.time.Duration;

import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.buurman.config.models.PostHogProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class PostHogConfiguration {

  private final PostHogProperties properties;

  @Bean
  public @Nullable PostHogFlagClient postHogFlagClient() {
    String apiKey = properties.projectApiKey();
    if (apiKey == null || apiKey.isBlank()) {
      log.warn("PostHog project API key not configured — all feature flags will default to OFF");
      return null;
    }

    Duration cacheTtl = Duration.ofSeconds(properties.pollIntervalSeconds());
    PostHogFlagClient client = new PostHogFlagClient(properties.host(), apiKey, cacheTtl);
    log.info(
        "PostHog flag client initialized (host={}, cacheTtl={}s)",
        properties.host(),
        properties.pollIntervalSeconds());
    return client;
  }
}
