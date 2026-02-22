package com.buurman.dto.response.backoffice;

import org.jspecify.annotations.Nullable;

public record LoggerConfigurationResponse(
    String name, @Nullable String configuredLevel, String effectiveLevel) {}
