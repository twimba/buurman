package com.buurman.dto.response;

import org.jspecify.annotations.Nullable;

public record TeamPreferencesResponse(PaymentSettings payments, RegionalSettings regional) {

  public record PaymentSettings(int paymentsAheadCount, boolean autoGenerationEnabled) {}

  public record RegionalSettings(
      @Nullable String defaultCurrency,
      @Nullable String defaultCountry,
      @Nullable String timezone,
      @Nullable String dateFormat,
      @Nullable String fiscalYearStartMonth) {}
}
