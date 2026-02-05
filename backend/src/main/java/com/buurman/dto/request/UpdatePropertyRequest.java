package com.buurman.dto.request;

import com.buurman.domain.Property;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdatePropertyRequest(
        @NotBlank(message = "Street is required")
        String street,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "Postal code is required")
        String postalCode,

        @NotBlank(message = "Country is required")
        String country,

        BigDecimal latitude,

        BigDecimal longitude,

        @Min(value = 0, message = "Bedrooms must be non-negative")
        Integer bedrooms,

        @Min(value = 0, message = "Bathrooms must be non-negative")
        Integer bathrooms,

        @Positive(message = "Square meters must be positive")
        BigDecimal squareMeters,

        @NotNull(message = "Property type is required")
        Property.PropertyType propertyType,

        @NotNull(message = "Status is required")
        Property.PropertyStatus status
) {}
