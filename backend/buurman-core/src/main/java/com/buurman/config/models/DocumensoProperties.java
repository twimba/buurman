package com.buurman.config.models;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.buurman.util.SkipTestCoverage;

/**
 * {@code publicUrl} is the browser-reachable Documenso origin used to build signing links. It
 * differs from {@code baseUrl} whenever the API is reached over an internal host (docker); empty
 * means "same as baseUrl".
 */
@ConfigurationProperties(prefix = "documenso")
@SkipTestCoverage
public record DocumensoProperties(
    String baseUrl, String apiKey, Optional<String> webhookSecret, Optional<String> publicUrl) {

  public DocumensoProperties {
    webhookSecret = Optional.ofNullable(webhookSecret).flatMap(o -> o);
    publicUrl = Optional.ofNullable(publicUrl).flatMap(o -> o).filter(u -> !u.isBlank());
  }
}
