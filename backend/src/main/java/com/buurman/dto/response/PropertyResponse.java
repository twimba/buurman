package com.buurman.dto.response;

import com.buurman.domain.Property;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PropertyResponse(
        UUID id,
        String identifier,
        UUID teamId,
        String street,
        String city,
        String postalCode,
        String country,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer bedrooms,
        Integer bathrooms,
        BigDecimal squareMeters,
        Property.PropertyType propertyType,
        Property.PropertyStatus status,
        String mainPhotoUrl,
        Instant createdAt,
        Instant updatedAt
) {}
