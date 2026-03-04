package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.identifier.AmenityIdentifier;

import jakarta.validation.constraints.NotNull;

public record PropertyAmenityRequest(
    @NotNull(message = "Amenity identifier is required") AmenityIdentifier amenityIdentifier,
    Optional<String> notes) {}
