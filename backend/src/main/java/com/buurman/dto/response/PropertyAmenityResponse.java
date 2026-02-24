package com.buurman.dto.response;

import java.util.Optional;

public record PropertyAmenityResponse(
    String amenityIdentifier,
    String amenityName,
    String amenityCategory,
    Optional<String> amenityIcon,
    Optional<String> notes) {}
