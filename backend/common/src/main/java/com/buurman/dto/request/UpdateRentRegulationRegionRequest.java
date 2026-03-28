package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record UpdateRentRegulationRegionRequest(
    @NotBlank @Size(max = 100) String regionName, Optional<String> summary) {}
