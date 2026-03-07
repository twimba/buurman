package com.buurman.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.buurman.util.MoneyAmount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyAcquisition {

  public enum AcquisitionType {
    PURCHASE,
    INHERITANCE,
    GIFT,
    FORECLOSURE,
    AUCTION,
    OTHER
  }

  public enum DepreciationMethod {
    STRAIGHT_LINE,
    DECLINING_BALANCE,
    NONE
  }

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID propertyId;
  private UUID teamId;
  private AcquisitionType acquisitionType;
  @Builder.Default private Optional<LocalDate> acquisitionDate = Optional.empty();
  @Builder.Default private Optional<MoneyAmount> purchasePrice = Optional.empty();
  @Builder.Default private Optional<MoneyAmount> closingCosts = Optional.empty();
  @Builder.Default private Optional<MoneyAmount> renovationCosts = Optional.empty();
  @Builder.Default private Optional<MoneyAmount> landValue = Optional.empty();
  @Builder.Default private Optional<DepreciationMethod> depreciationMethod = Optional.empty();
  @Builder.Default private Optional<Integer> depreciationYears = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
