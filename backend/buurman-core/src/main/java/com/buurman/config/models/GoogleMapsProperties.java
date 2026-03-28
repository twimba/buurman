package com.buurman.config.models;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.buurman.util.SkipTestCoverage;

@ConfigurationProperties(prefix = "google.maps")
@SkipTestCoverage
public record GoogleMapsProperties(String apiKey) {}
