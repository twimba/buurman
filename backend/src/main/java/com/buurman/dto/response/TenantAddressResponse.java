package com.buurman.dto.response;

import java.time.Instant;

import com.buurman.domain.TenantAddress;

public record TenantAddressResponse(
    String identifier,
    String street,
    String city,
    String postalCode,
    String country,
    TenantAddress.AddressType addressType,
    TenantAddress.AddressStatus status,
    Double latitude,
    Double longitude,
    String geocodeAccuracy,
    Instant createdAt,
    Instant updatedAt) {}
