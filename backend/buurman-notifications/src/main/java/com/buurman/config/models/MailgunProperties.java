package com.buurman.config.models;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.buurman.util.SkipTestCoverage;

@ConfigurationProperties(prefix = "mailgun")
@SkipTestCoverage
public record MailgunProperties(
    String apiKey,
    String domain,
    String fromEmail,
    String fromName,
    Optional<String> webhookSigningKey,
    boolean euRegion) {

  public MailgunProperties {
    webhookSigningKey = Optional.ofNullable(webhookSigningKey).flatMap(o -> o);
  }
}
