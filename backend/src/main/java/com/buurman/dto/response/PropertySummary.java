package com.buurman.dto.response;

import com.buurman.domain.Property;
import com.buurman.domain.Ulid;

public record PropertySummary(
    Ulid identifier,
    String street,
    String city,
    String postalCode,
    Property.PropertyCategory propertyCategory,
    Property.PropertyType propertyType,
    Property.PropertyStatus status) {}
