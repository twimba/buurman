package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.TenantAddress;
import com.buurman.domain.TenantAddress.AddressStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTenantAddressRequest(
    @NotBlank(message = "Street is required") String street,
    @NotBlank(message = "City is required") String city,
    Optional<String> postalCode,
    @NotBlank(message = "Country code is required") String countryCode,
    @NotNull(message = "Address type is required") TenantAddress.AddressType addressType,
    Optional<AddressStatus> status,
    Optional<Double> latitude,
    Optional<Double> longitude,
    Optional<String> geocodeAccuracy) {}
