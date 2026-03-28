package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record ResidentialDetailsResponse(
    Optional<Integer> bedrooms,
    Optional<Integer> bathrooms,
    Optional<Boolean> furnished,
    Optional<String> petPolicy) {}
