package com.buurman.dto.request;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.Min;

public record ResidentialDetailsRequest(
    @Nullable @Min(value = 0, message = "Bedrooms must be non-negative") Integer bedrooms,
    @Nullable @Min(value = 0, message = "Bathrooms must be non-negative") Integer bathrooms,
    @Nullable Boolean furnished,
    @Nullable String petPolicy) {}
