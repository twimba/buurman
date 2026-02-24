package com.buurman.dto.request;

import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.Min;

public record ResidentialDetailsRequest(
    @Min(value = 0, message = "Bedrooms must be non-negative") Optional<Integer> bedrooms,
    @Min(value = 0, message = "Bathrooms must be non-negative") Optional<Integer> bathrooms,
    Optional<Boolean> furnished,
    Optional<String> petPolicy) {

  public ResidentialDetailsRequest {
    bedrooms = Objects.requireNonNullElse(bedrooms, Optional.empty());
    bathrooms = Objects.requireNonNullElse(bathrooms, Optional.empty());
    furnished = Objects.requireNonNullElse(furnished, Optional.empty());
    petPolicy = Objects.requireNonNullElse(petPolicy, Optional.empty());
  }
}
