package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import org.jspecify.annotations.Nullable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamPreferences {

  private UUID id;
  private UUID teamId;

  // Payment settings
  @Builder.Default private int paymentsAheadCount = 3;
  @Builder.Default private boolean autoGenerationEnabled = true;

  // Regional settings
  private @Nullable String defaultCurrency;
  @Builder.Default private String defaultCountry = "Netherlands";
  @Builder.Default private String timezone = "Europe/Amsterdam";
  @Builder.Default private String dateFormat = "DD/MM/YYYY";
  @Builder.Default private String fiscalYearStartMonth = "01";

  // Audit
  private Instant createdAt;
  private Instant updatedAt;
}
