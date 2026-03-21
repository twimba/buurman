package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.util.Generated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Generated
public record UpdateTeamSettingsRequest(
    @Valid Optional<PaymentSettings> payments, @Valid Optional<RegionalSettings> regional) {

  public record PaymentSettings(
      @NotNull(message = "Payments ahead count is required") @Min(value = 1, message = "Payments ahead count must be at least 1") @Max(value = 12, message = "Payments ahead count must not exceed 12") Integer paymentsAheadCount,
      @NotNull(message = "Auto generation enabled flag is required") Boolean autoGenerationEnabled) {}

  public record RegionalSettings(
      Optional<@Size(min = 3, max = 3, message = "Currency code must be 3 characters") String>
          defaultCurrency,
      Optional<@Size(max = 2, message = "Country code must be 2 characters") String>
          defaultCountryCode,
      Optional<@Size(max = 50, message = "Timezone must not exceed 50 characters") String> timezone,
      Optional<@Size(max = 20, message = "Date format must not exceed 20 characters") String>
          dateFormat,
      Optional<@Size(min = 2, max = 2, message = "Fiscal year start month must be 2 digits") String>
          fiscalYearStartMonth) {}
}
