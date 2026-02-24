package com.buurman.dto.response.backoffice;

import java.util.Optional;

public record LoggerConfigurationResponse(
    String name, Optional<String> configuredLevel, String effectiveLevel) {}
