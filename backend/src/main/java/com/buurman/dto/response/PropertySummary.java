package com.buurman.dto.response;

import com.buurman.domain.Property;

public record PropertySummary(
    String identifier,
    String street,
    String city,
    String postalCode,
    Property.PropertyCategory propertyCategory,
    Property.PropertyType propertyType,
    Property.PropertyStatus status) {}
