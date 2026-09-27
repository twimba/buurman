package com.buurman.dto.response;

import org.jspecify.annotations.Nullable;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record UnitResidentialDetailsResponse(
    @Nullable Integer bedrooms,
    @Nullable Integer bathrooms,
    boolean furnished,
    @Nullable String petPolicy) {}
