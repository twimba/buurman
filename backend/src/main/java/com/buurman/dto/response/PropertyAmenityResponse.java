package com.buurman.dto.response;

import org.jspecify.annotations.Nullable;

public record PropertyAmenityResponse(
    String amenityIdentifier,
    String amenityName,
    String amenityCategory,
    @Nullable String amenityIcon,
    @Nullable String notes) {}
