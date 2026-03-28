package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.ContactAddress;
import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record ContactAddressResponse(
    Sid identifier,
    String street,
    String city,
    String postalCode,
    String countryCode,
    ContactAddress.AddressType addressType,
    ContactAddress.AddressStatus status,
    Optional<Double> latitude,
    Optional<Double> longitude,
    Optional<String> geocodeAccuracy,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
