package com.buurman.dto.response;

import com.buurman.domain.Property;

import java.util.UUID;

public record PropertySummary(
        UUID id,
        String identifier,
        String street,
        String city,
        String postalCode,
        Property.PropertyType propertyType,
        Property.PropertyStatus status
) {}
