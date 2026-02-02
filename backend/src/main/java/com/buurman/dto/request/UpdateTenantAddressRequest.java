package com.buurman.dto.request;

import com.buurman.domain.TenantAddress;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateTenantAddressRequest(
        @NotBlank(message = "Street is required")
        String street,

        @NotBlank(message = "City is required")
        String city,

        String postalCode,

        @NotBlank(message = "Country is required")
        String country,

        @NotNull(message = "Address type is required")
        TenantAddress.AddressType addressType,

        @NotNull(message = "Status is required")
        TenantAddress.AddressStatus status,

        Double latitude,

        Double longitude
) {
}
