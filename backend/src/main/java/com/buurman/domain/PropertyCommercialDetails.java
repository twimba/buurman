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
public class PropertyCommercialDetails {

  private UUID id;
  private UUID propertyId;
  private UUID teamId;
  @Builder.Default private Optional<BigDecimal> usableAreaValue = Optional.empty();
  @Builder.Default private Optional<String> usableAreaUnit = Optional.empty();
  @Builder.Default private Optional<BigDecimal> commonAreaValue = Optional.empty();
  @Builder.Default private Optional<String> commonAreaUnit = Optional.empty();
  @Builder.Default private Optional<Integer> floorLevel = Optional.empty();
  @Builder.Default private Optional<BigDecimal> ceilingHeightValue = Optional.empty();
  @Builder.Default private Optional<String> ceilingHeightUnit = Optional.empty();
  @Builder.Default private Optional<Boolean> hasStorefront = Optional.empty();
  @Builder.Default private Optional<Boolean> hasSignageRights = Optional.empty();
  @Builder.Default private Optional<String> zoningClassification = Optional.empty();
  @Builder.Default private Optional<Integer> maxOccupancy = Optional.empty();
  @Builder.Default private Optional<Integer> restroomCount = Optional.empty();
  @Builder.Default private Optional<Boolean> hasKitchenFacility = Optional.empty();
  @Builder.Default private Optional<Boolean> accessibilityCompliant = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
}
