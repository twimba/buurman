package com.buurman.dto.request;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.TenantAddress;
import com.buurman.domain.TenantAddress.AddressStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTenantAddressRequest(
    @NotBlank(message = "Street is required") String street,
    @NotBlank(message = "City is required") String city,
    @Nullable String postalCode,
    @NotBlank(message = "Country is required") String country,
    @NotNull(message = "Address type is required") TenantAddress.AddressType addressType,
    @Nullable AddressStatus status,
    @Nullable Double latitude,
    @Nullable Double longitude,
    @Nullable String geocodeAccuracy) {}
