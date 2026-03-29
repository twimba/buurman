package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record CreateRentRegulationCountryRequest(
    @NotBlank @Size(min = 2, max = 3) String countryCode,
    @NotBlank @Size(max = 100) String countryName,
    boolean hasRegionalRegulations,
    Optional<String> summary) {}
