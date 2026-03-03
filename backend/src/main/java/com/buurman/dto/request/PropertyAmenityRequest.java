package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.Ulid;

import jakarta.validation.constraints.NotNull;

public record PropertyAmenityRequest(
    @NotNull(message = "Amenity identifier is required") Ulid amenityIdentifier,
    Optional<String> notes) {}
