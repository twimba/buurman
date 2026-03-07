package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.domain.TenantAddress;

public record TenantAddressResponse(
    Sid identifier,
    String street,
    String city,
    String postalCode,
    String countryCode,
    TenantAddress.AddressType addressType,
    TenantAddress.AddressStatus status,
    Optional<Double> latitude,
    Optional<Double> longitude,
    Optional<String> geocodeAccuracy,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
