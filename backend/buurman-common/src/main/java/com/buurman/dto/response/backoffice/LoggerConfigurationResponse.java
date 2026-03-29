package com.buurman.dto.response.backoffice;

import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record LoggerConfigurationResponse(
    String name, Optional<String> configuredLevel, String effectiveLevel) {}
