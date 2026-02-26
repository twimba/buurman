package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyValuation {

  public enum ValuationType {
    MARKET,
    APPRAISAL,
    TAX_ASSESSED,
    PURCHASE,
    INSURANCE,
    USER_ESTIMATE
  }

  private UUID id;
  private String identifier;
  private UUID propertyId;
  private UUID teamId;
  private ValuationType valuationType;
  private LocalDate valuationDate;
  private BigDecimal amount;
  private String currency;
  @Builder.Default private Optional<String> source = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
