package com.buurman.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "buurman.demo")
public record DemoDataProperties(
        boolean enabled,
        String apiKey,
        boolean autoRegenerate,
        String cron
) {}
