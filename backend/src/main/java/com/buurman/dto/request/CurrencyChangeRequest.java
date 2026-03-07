package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Optional;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CurrencyChangeRequest(
    @NotBlank(message = "New currency is required") String newCurrency,
    @NotNull(message = "Change mode is required") ChangeMode mode,
    Optional<@Positive(message = "Conversion rate must be positive") BigDecimal> conversionRate) {

  public enum ChangeMode {
    RELABEL,
    CONVERT
  }
}
