package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.domain.Sid;

public record PropertyAmenityResponse(
    Sid amenityIdentifier,
    String amenityName,
    String amenityCategory,
    Optional<String> amenityIcon,
    Optional<String> notes) {}
