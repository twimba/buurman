package com.buurman.dto.response;

public record TeamPreferencesResponse(PaymentSettings payments, RegionalSettings regional) {

  public record PaymentSettings(int paymentsAheadCount, boolean autoGenerationEnabled) {}

  public record RegionalSettings(
      String defaultCurrency,
      String defaultCountry,
      String timezone,
      String dateFormat,
      String fiscalYearStartMonth) {}
}
