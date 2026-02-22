package com.buurman.dto.request;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.NotBlank;

public record PropertyAmenityRequest(
    @NotBlank(message = "Amenity identifier is required") String amenityIdentifier,
    @Nullable String notes) {}
