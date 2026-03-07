package com.buurman.dto.request;

import java.util.Optional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRentRegulationRegionRequest(
    @NotBlank @Size(max = 20) String regionCode,
    @NotBlank @Size(max = 100) String regionName,
    Optional<String> summary) {}
