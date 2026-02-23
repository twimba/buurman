package com.buurman.config.models;

import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "twilio")
public record TwilioProperties(
    String accountSid,
    String authToken,
    String fromNumber,
    @Nullable String messagingServiceSid,
    @Nullable String statusCallbackUrl) {}
