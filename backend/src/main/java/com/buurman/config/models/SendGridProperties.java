package com.buurman.config.models;

import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sendgrid")
public record SendGridProperties(
    String apiKey, String fromEmail, String fromName, Optional<String> webhookVerificationKey) {}
