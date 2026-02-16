package com.buurman.dto.response.backoffice;

public record LoggerConfigurationResponse(
    String name, String configuredLevel, String effectiveLevel) {}
