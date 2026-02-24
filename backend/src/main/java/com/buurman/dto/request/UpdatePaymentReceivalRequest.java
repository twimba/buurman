package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdatePaymentReceivalRequest(
    @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal amount,
    @NotNull(message = "Receival date is required") LocalDate receivalDate,
    Optional<String> notes) {
  public UpdatePaymentReceivalRequest {
    notes = Objects.requireNonNullElse(notes, Optional.empty());
  }
}
