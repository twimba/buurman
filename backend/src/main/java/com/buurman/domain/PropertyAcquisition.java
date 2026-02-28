package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
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

  @Schema(description = "How the property was acquired")
  public enum AcquisitionType {
    PURCHASE,
    INHERITANCE,
    GIFT,
    FORECLOSURE,
    AUCTION,
    OTHER
  }

  @Schema(description = "Accounting method used for property depreciation")
  public enum DepreciationMethod {
    STRAIGHT_LINE,
    DECLINING_BALANCE,
    NONE
  }

  private UUID id;
  private String identifier;
  private UUID propertyId;
  private UUID teamId;
  private AcquisitionType acquisitionType;
  @Builder.Default private Optional<LocalDate> acquisitionDate = Optional.empty();
  @Builder.Default private Optional<BigDecimal> purchasePrice = Optional.empty();
  @Builder.Default private Optional<String> purchasePriceCurrency = Optional.empty();
  @Builder.Default private Optional<BigDecimal> closingCosts = Optional.empty();
  @Builder.Default private Optional<String> closingCostsCurrency = Optional.empty();
  @Builder.Default private Optional<BigDecimal> renovationCosts = Optional.empty();
  @Builder.Default private Optional<String> renovationCostsCurrency = Optional.empty();
  @Builder.Default private Optional<BigDecimal> landValue = Optional.empty();
  @Builder.Default private Optional<String> landValueCurrency = Optional.empty();
  @Builder.Default private Optional<DepreciationMethod> depreciationMethod = Optional.empty();
  @Builder.Default private Optional<Integer> depreciationYears = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
