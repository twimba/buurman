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
public class PropertyTax {

  public enum TaxType {
    PROPERTY,
    MUNICIPAL,
    STATE,
    LOCAL,
    LAND,
    SPECIAL_ASSESSMENT,
    OTHER
  }

  public enum TaxStatus {
    ACTIVE,
    EXPIRED,
    EXEMPT
  }

  private UUID id;
  private String identifier;
  private UUID propertyId;
  private UUID teamId;
  private TaxType taxType;
  @Builder.Default private Optional<String> authority = Optional.empty();
  private BigDecimal annualAmount;
  private String currency;
  private String paymentFrequency;
  @Builder.Default private Optional<String> dueMonths = Optional.empty();
  @Builder.Default private Optional<Integer> taxYear = Optional.empty();
  @Builder.Default private Optional<LocalDate> startDate = Optional.empty();
  @Builder.Default private Optional<LocalDate> endDate = Optional.empty();
  private TaxStatus status;
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
