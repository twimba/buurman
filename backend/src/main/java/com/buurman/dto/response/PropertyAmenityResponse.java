package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.domain.Ulid;

public record PropertyAmenityResponse(
    Ulid amenityIdentifier,
    String amenityName,
    String amenityCategory,
    Optional<String> amenityIcon,
    Optional<String> notes) {}
