package com.buurman.dto.request;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.NotNull;

public record MarkPaidRequest(
    @NotNull(message = "Payment date is required") LocalDate paymentDate, Optional<String> notes) {
  public MarkPaidRequest {
    notes = Objects.requireNonNullElse(notes, Optional.empty());
  }
}
