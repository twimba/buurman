package com.buurman.dto.request;

import jakarta.validation.constraints.Min;

public record ResidentialDetailsRequest(
    @Min(value = 0, message = "Bedrooms must be non-negative") Integer bedrooms,
    @Min(value = 0, message = "Bathrooms must be non-negative") Integer bathrooms,
    Boolean furnished,
    String petPolicy) {}
