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

/** A separately-lettable dwelling within a {@link Property}. */
@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Unit {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID propertyId;

  // Identity within the building
  @Builder.Default private Optional<String> name = Optional.empty();
  private String unitNumber;
  @Builder.Default private Optional<Integer> floor = Optional.empty();
  @Builder.Default private int sortOrder = 0;
  private UnitType unitType;
  private UnitStatus status;

  /**
   * True while this unit is the auto-created stand-in for a single-unit property. A UI-visibility
   * hint only — never a filter in an aggregation.
   */
  @Builder.Default private boolean implicit = false;

  // Valuation & allocation shares
  @Builder.Default private Optional<BigDecimal> wozValue = Optional.empty();
  @Builder.Default private Optional<String> wozValueCurrency = Optional.empty();
  @Builder.Default private Optional<BigDecimal> wozSharePct = Optional.empty();
  @Builder.Default private Optional<BigDecimal> allocationShare = Optional.empty();

  // Specifications
  @Builder.Default private Optional<BigDecimal> areaValue = Optional.empty();
  @Builder.Default private Optional<String> areaUnit = Optional.empty();

  // Energy & climate
  @Builder.Default private Optional<String> energyEfficiencyRating = Optional.empty();
  @Builder.Default private Optional<LocalDate> energyCertificateExpiryDate = Optional.empty();
  @Builder.Default private Optional<String> heatingType = Optional.empty();
  @Builder.Default private Optional<String> coolingType = Optional.empty();
  @Builder.Default private Optional<String> hotWaterSystem = Optional.empty();
  @Builder.Default private Optional<String> insulationNotes = Optional.empty();

  // Finishes
  @Builder.Default private Optional<String> flooringType = Optional.empty();
  @Builder.Default private Optional<String> windowType = Optional.empty();

  // Safety
  @Builder.Default private Optional<Boolean> hasSmokeDetectors = Optional.empty();
  @Builder.Default private Optional<Boolean> hasCoDetectors = Optional.empty();
  @Builder.Default private Optional<Boolean> hasFireExtinguisher = Optional.empty();

  // Accessibility
  @Builder.Default private Optional<Boolean> hasAdaptedBathroom = Optional.empty();
  @Builder.Default private Optional<String> accessibilityNotes = Optional.empty();

  // Audit
  @Builder.Default private Optional<Instant> createdAt = Optional.empty();
  @Builder.Default private Optional<Instant> updatedAt = Optional.empty();
  @Builder.Default private Optional<UUID> createdBy = Optional.empty();
  @Builder.Default private Optional<UUID> updatedBy = Optional.empty();
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
