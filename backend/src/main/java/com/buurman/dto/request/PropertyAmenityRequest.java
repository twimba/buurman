package com.buurman.dto.request;

import jakarta.validation.constraints.NotBlank;

public record PropertyAmenityRequest(
    @NotBlank(message = "Amenity identifier is required") String amenityIdentifier, String notes) {}
