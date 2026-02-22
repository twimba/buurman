package com.buurman.dto.response;

import org.jspecify.annotations.Nullable;

public record ResidentialDetailsResponse(
    @Nullable Integer bedrooms,
    @Nullable Integer bathrooms,
    @Nullable Boolean furnished,
    @Nullable String petPolicy) {}
