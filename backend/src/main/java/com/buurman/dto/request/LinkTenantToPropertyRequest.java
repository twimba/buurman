package com.buurman.dto.request;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;

public record LinkTenantToPropertyRequest(
    @NotNull(message = "Property identifier is required") String propertyIdentifier,
    Optional<Instant> movedInAt) {

  public LinkTenantToPropertyRequest {
    movedInAt = Objects.requireNonNullElse(movedInAt, Optional.empty());
  }
}
