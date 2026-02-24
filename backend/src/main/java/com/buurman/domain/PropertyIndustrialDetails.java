package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
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
public class PropertyIndustrialDetails {

  private UUID id;
  private UUID propertyId;
  private UUID teamId;
  @Builder.Default private Optional<BigDecimal> clearHeightM = Optional.empty();
  @Builder.Default private Optional<Integer> loadingDocks = Optional.empty();
  @Builder.Default private Optional<Integer> driveInDoors = Optional.empty();
  @Builder.Default private Optional<BigDecimal> floorLoadCapacityKgSqm = Optional.empty();
  @Builder.Default private Optional<Integer> powerCapacityKva = Optional.empty();
  @Builder.Default private Optional<Boolean> hasThreePhasePower = Optional.empty();
  @Builder.Default private Optional<Boolean> hasCrane = Optional.empty();
  @Builder.Default private Optional<BigDecimal> craneCapacityTons = Optional.empty();
  @Builder.Default private Optional<Boolean> hasHazmatCertification = Optional.empty();
  @Builder.Default private Optional<Boolean> hasVentilationSystem = Optional.empty();
  @Builder.Default private Optional<Boolean> hasClimateControl = Optional.empty();
  @Builder.Default private Optional<BigDecimal> yardAreaValue = Optional.empty();
  @Builder.Default private Optional<String> yardAreaUnit = Optional.empty();
  @Builder.Default private Optional<String> zoningClassification = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
}
