package com.buurman.dto.response;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.UnitStatus;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record PropertySummary(
    Sid identifier,
    String street,
    String city,
    String postalCode,
    Property.PropertyCategory propertyCategory,
    Property.PropertyType propertyType,
    // TODO(BUUR-106 Task 12): status moved from properties to units; null until callers can
    // derive it from the property's unit(s).
    @Nullable UnitStatus status) {}
