package com.buurman.dto.request;

import java.util.Objects;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTeamSettingsRequest(
    @Valid Optional<PaymentSettings> payments, @Valid Optional<RegionalSettings> regional) {
  public UpdateTeamSettingsRequest {
    payments = Objects.requireNonNullElse(payments, Optional.empty());
    regional = Objects.requireNonNullElse(regional, Optional.empty());
  }

  public record PaymentSettings(
      @NotNull(message = "Payments ahead count is required") @Min(value = 1, message = "Payments ahead count must be at least 1") @Max(value = 12, message = "Payments ahead count must not exceed 12") Integer paymentsAheadCount,
      @NotNull(message = "Auto generation enabled flag is required") Boolean autoGenerationEnabled) {}

  public record RegionalSettings(
      @Size(min = 3, max = 3, message = "Currency code must be 3 characters") Optional<String> defaultCurrency,
      @Size(max = 100, message = "Country name must not exceed 100 characters") Optional<String> defaultCountry,
      @Size(max = 50, message = "Timezone must not exceed 50 characters") Optional<String> timezone,
      @Size(max = 20, message = "Date format must not exceed 20 characters") Optional<String> dateFormat,
      @Size(min = 2, max = 2, message = "Fiscal year start month must be 2 digits") Optional<String> fiscalYearStartMonth) {
    public RegionalSettings {
      defaultCurrency = Objects.requireNonNullElse(defaultCurrency, Optional.empty());
      defaultCountry = Objects.requireNonNullElse(defaultCountry, Optional.empty());
      timezone = Objects.requireNonNullElse(timezone, Optional.empty());
      dateFormat = Objects.requireNonNullElse(dateFormat, Optional.empty());
      fiscalYearStartMonth = Objects.requireNonNullElse(fiscalYearStartMonth, Optional.empty());
    }
  }
}
