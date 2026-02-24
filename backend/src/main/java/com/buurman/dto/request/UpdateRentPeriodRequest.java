package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record UpdateRentPeriodRequest(
    @NotNull @DecimalMin(value = "0.01") BigDecimal rentAmount,
    @NotNull LocalDate effectiveFrom,
    Optional<String> notes) {

  public UpdateRentPeriodRequest {
    notes = Objects.requireNonNullElse(notes, Optional.empty());
  }
}
