package com.buurman.config.models;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "buurman.demo")
public record DemoDataProperties(
        boolean enabled,
        String apiKey,
        String cron
) {}
