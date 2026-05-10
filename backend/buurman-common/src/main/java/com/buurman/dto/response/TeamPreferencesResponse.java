package com.buurman.dto.response;

import java.util.Optional;

public record TeamPreferencesResponse(PaymentSettings payments, RegionalSettings regional) {

  public record PaymentSettings(int paymentsAheadCount, boolean autoGenerationEnabled) {}

  public record RegionalSettings(
      String defaultCurrency,
      Optional<String> defaultCountryCode,
      Optional<String> timezone,
      Optional<String> dateFormat,
      Optional<String> fiscalYearStartMonth,
      String defaultLanguage) {}
}
