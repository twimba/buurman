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
public class PropertyFee {

  public enum FeeType {
    HOA,
    MANAGEMENT,
    MAINTENANCE_RESERVE,
    CLEANING,
    GARDENING,
    SECURITY,
    WASTE_MANAGEMENT,
    WATER,
    UTILITIES,
    OTHER
  }

  public enum FeeStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED
  }

  private UUID id;
  @Builder.Default private Optional<Ulid> identifier = Optional.empty();
  private UUID propertyId;
  private UUID teamId;
  private FeeType feeType;
  @Builder.Default private Optional<String> name = Optional.empty();
  private BigDecimal annualAmount;
  private String currency;
  private String paymentFrequency;
  @Builder.Default private Optional<String> dueMonths = Optional.empty();
  @Builder.Default private Optional<LocalDate> startDate = Optional.empty();
  @Builder.Default private Optional<LocalDate> endDate = Optional.empty();
  private FeeStatus status;
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
