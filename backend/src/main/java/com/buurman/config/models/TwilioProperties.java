package com.buurman.config.models;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "twilio")
public record TwilioProperties(
    String accountSid,
    String authToken,
    String fromNumber,
    String messagingServiceSid,
    String statusCallbackUrl) {}
