package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.TenantAddress;
import com.buurman.domain.Ulid;

public record TenantAddressResponse(
    Ulid identifier,
    String street,
    String city,
    String postalCode,
    String country,
    TenantAddress.AddressType addressType,
    TenantAddress.AddressStatus status,
    Optional<Double> latitude,
    Optional<Double> longitude,
    Optional<String> geocodeAccuracy,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
