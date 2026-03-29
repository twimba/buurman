package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record CreateCountryRegulationRequestRequest(
    @NotBlank @Size(max = 255) String countryName, Optional<@Size(max = 1000) String> notes) {}
