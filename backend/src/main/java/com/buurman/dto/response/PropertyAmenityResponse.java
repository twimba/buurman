package com.buurman.dto.response;

public record PropertyAmenityResponse(
    String amenityIdentifier,
    String amenityName,
    String amenityCategory,
    String amenityIcon,
    String notes) {}
