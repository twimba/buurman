package com.buurman.dto.request;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.TenantAddress;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateTenantAddressRequest(
    @NotBlank(message = "Street is required") String street,
    @NotBlank(message = "City is required") String city,
    @Nullable String postalCode,
    @NotBlank(message = "Country is required") String country,
    @NotNull(message = "Address type is required") TenantAddress.AddressType addressType,
    @NotNull(message = "Status is required") TenantAddress.AddressStatus status,
    @Nullable Double latitude,
    @Nullable Double longitude,
    @Nullable String geocodeAccuracy) {}
