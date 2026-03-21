package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.TenantAddress;
import com.buurman.util.Generated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Generated
public record UpdateTenantAddressRequest(
    @NotBlank(message = "Street is required") String street,
    @NotBlank(message = "City is required") String city,
    Optional<String> postalCode,
    @NotBlank(message = "Country code is required") String countryCode,
    @NotNull(message = "Address type is required") TenantAddress.AddressType addressType,
    @NotNull(message = "Status is required") TenantAddress.AddressStatus status,
    Optional<Double> latitude,
    Optional<Double> longitude,
    Optional<String> geocodeAccuracy) {}
