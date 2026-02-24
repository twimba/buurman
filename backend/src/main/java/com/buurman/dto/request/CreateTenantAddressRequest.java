package com.buurman.dto.request;

import java.util.Objects;
import java.util.Optional;

import com.buurman.domain.TenantAddress;
import com.buurman.domain.TenantAddress.AddressStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateTenantAddressRequest(
    @NotBlank(message = "Street is required") String street,
    @NotBlank(message = "City is required") String city,
    Optional<String> postalCode,
    @NotBlank(message = "Country is required") String country,
    @NotNull(message = "Address type is required") TenantAddress.AddressType addressType,
    Optional<AddressStatus> status,
    Optional<Double> latitude,
    Optional<Double> longitude,
    Optional<String> geocodeAccuracy) {
  public CreateTenantAddressRequest {
    postalCode = Objects.requireNonNullElse(postalCode, Optional.empty());
    status = Objects.requireNonNullElse(status, Optional.empty());
    latitude = Objects.requireNonNullElse(latitude, Optional.empty());
    longitude = Objects.requireNonNullElse(longitude, Optional.empty());
    geocodeAccuracy = Objects.requireNonNullElse(geocodeAccuracy, Optional.empty());
  }
}
