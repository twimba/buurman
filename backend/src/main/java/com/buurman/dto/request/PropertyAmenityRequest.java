package com.buurman.dto.request;

import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.NotBlank;

public record PropertyAmenityRequest(
    @NotBlank(message = "Amenity identifier is required") String amenityIdentifier,
    Optional<String> notes) {

  public PropertyAmenityRequest {
    notes = Objects.requireNonNullElse(notes, Optional.empty());
  }
}
