package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record CreateRentRegulationRegionRequest(
    @NotBlank @Size(max = 20) String regionCode,
    @NotBlank @Size(max = 100) String regionName,
    Optional<String> summary) {}
