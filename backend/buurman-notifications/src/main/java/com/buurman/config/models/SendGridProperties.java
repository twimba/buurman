package com.buurman.config.models;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.buurman.util.SkipTestCoverage;

@ConfigurationProperties(prefix = "sendgrid")
@SkipTestCoverage
public record SendGridProperties(
    String apiKey, String fromEmail, String fromName, Optional<String> webhookVerificationKey) {

  public SendGridProperties {
    webhookVerificationKey = Optional.ofNullable(webhookVerificationKey).flatMap(o -> o);
  }
}
