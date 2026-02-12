package com.buurman.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "twilio")
public record TwilioProperties(
        String accountSid,
        String authToken,
        String fromNumber,
        String statusCallbackUrl
) {}
