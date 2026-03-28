package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;

@SkipTestCoverage
public record GeocodeRequest(
    @NotBlank String street,
    @NotBlank String city,
    Optional<String> postalCode,
    @NotBlank String countryCode) {}
