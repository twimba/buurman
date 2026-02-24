package com.buurman.dto.response;

import java.util.Optional;

public record TeamPreferencesResponse(PaymentSettings payments, RegionalSettings regional) {

  public record PaymentSettings(int paymentsAheadCount, boolean autoGenerationEnabled) {}

  public record RegionalSettings(
      Optional<String> defaultCurrency,
      Optional<String> defaultCountry,
      Optional<String> timezone,
      Optional<String> dateFormat,
      Optional<String> fiscalYearStartMonth) {}
}
