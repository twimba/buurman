package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;

@SkipTestCoverage
public record PropertyOutdoorAreaRequest(
    @NotBlank(message = "Type is required") String type,
    Optional<BigDecimal> areaValue,
    Optional<String> areaUnit) {}
