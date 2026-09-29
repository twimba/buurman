package com.buurman.config.models;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.buurman.util.SkipTestCoverage;

@ConfigurationProperties(prefix = "documenso")
@SkipTestCoverage
public record DocumensoProperties(String baseUrl, String apiKey, Optional<String> webhookSecret) {

  public DocumensoProperties {
    webhookSecret = Optional.ofNullable(webhookSecret).flatMap(o -> o);
  }
}
