package com.buurman.config.models;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.buurman.util.Generated;

@ConfigurationProperties(prefix = "google.maps")
@Generated
public record GoogleMapsProperties(String apiKey) {}
