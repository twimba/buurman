package com.buurman.dto.response;

import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record PropertySummary(
    Sid identifier,
    String street,
    String city,
    String postalCode,
    Property.PropertyCategory propertyCategory,
    Property.PropertyType propertyType,
    Property.PropertyStatus status) {}
