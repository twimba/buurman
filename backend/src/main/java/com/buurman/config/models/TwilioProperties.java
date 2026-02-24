package com.buurman.config.models;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "twilio")
public record TwilioProperties(
    String accountSid,
    String authToken,
    String fromNumber,
    Optional<String> messagingServiceSid,
    Optional<String> statusCallbackUrl) {

  public TwilioProperties {
    messagingServiceSid = Optional.ofNullable(messagingServiceSid).flatMap(o -> o);
    statusCallbackUrl = Optional.ofNullable(statusCallbackUrl).flatMap(o -> o);
  }
}
