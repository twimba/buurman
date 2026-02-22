package com.buurman.dto.response;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.TenantAddress;

public record TenantAddressResponse(
    String identifier,
    String street,
    String city,
    String postalCode,
    String country,
    TenantAddress.AddressType addressType,
    TenantAddress.AddressStatus status,
    @Nullable Double latitude,
    @Nullable Double longitude,
    @Nullable String geocodeAccuracy,
    Instant createdAt,
    @Nullable Instant updatedAt) {}
