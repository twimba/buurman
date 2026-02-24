package com.buurman.dto.request;

import java.util.Objects;
import java.util.Optional;

import com.buurman.domain.TenantAddress;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateTenantAddressRequest(
    @NotBlank(message = "Street is required") String street,
    @NotBlank(message = "City is required") String city,
    Optional<String> postalCode,
    @NotBlank(message = "Country is required") String country,
    @NotNull(message = "Address type is required") TenantAddress.AddressType addressType,
    @NotNull(message = "Status is required") TenantAddress.AddressStatus status,
    Optional<Double> latitude,
    Optional<Double> longitude,
    Optional<String> geocodeAccuracy) {
  public UpdateTenantAddressRequest {
    postalCode = Objects.requireNonNullElse(postalCode, Optional.empty());
    latitude = Objects.requireNonNullElse(latitude, Optional.empty());
    longitude = Objects.requireNonNullElse(longitude, Optional.empty());
    geocodeAccuracy = Objects.requireNonNullElse(geocodeAccuracy, Optional.empty());
  }
}
