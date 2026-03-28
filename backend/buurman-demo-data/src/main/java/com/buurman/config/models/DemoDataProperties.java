package com.buurman.config.models;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.buurman.util.SkipTestCoverage;

@ConfigurationProperties(prefix = "buurman.demo")
@SkipTestCoverage
public record DemoDataProperties(boolean enabled, String cron) {}
