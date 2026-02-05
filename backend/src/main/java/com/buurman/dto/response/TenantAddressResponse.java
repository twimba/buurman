package com.buurman.dto.response;

import com.buurman.domain.TenantAddress;

import java.time.Instant;

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
        Instant createdAt,
        Instant updatedAt
) {
}
