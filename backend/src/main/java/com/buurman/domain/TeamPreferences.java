package com.buurman.domain;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TeamPreferences {

  private UUID id;
  private UUID teamId;

  // Payment settings
  private int paymentsAheadCount = 3;
  private boolean autoGenerationEnabled = true;

  // Regional settings
  private String defaultCurrency;
  private String defaultCountry = "Netherlands";
  private String timezone = "Europe/Amsterdam";
  private String dateFormat = "DD/MM/YYYY";
  private String fiscalYearStartMonth = "01";

  // Audit
  private Instant createdAt;
  private Instant updatedAt;
}
