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
public class PropertyAgriculturalDetails {

  private UUID id;
  private UUID propertyId;
  private UUID teamId;
  @Builder.Default private Optional<BigDecimal> totalLandAreaValue = Optional.empty();
  @Builder.Default private Optional<String> totalLandAreaUnit = Optional.empty();
  @Builder.Default private Optional<BigDecimal> arableAreaValue = Optional.empty();
  @Builder.Default private Optional<String> arableAreaUnit = Optional.empty();
  @Builder.Default private Optional<String> soilType = Optional.empty();
  @Builder.Default private Optional<Boolean> hasWaterRights = Optional.empty();
  @Builder.Default private Optional<String> waterSource = Optional.empty();
  @Builder.Default private Optional<String> irrigationType = Optional.empty();
  @Builder.Default private Optional<String> fencingType = Optional.empty();
  @Builder.Default private Optional<Boolean> hasOutbuildings = Optional.empty();
  @Builder.Default private Optional<String> outbuildingDetails = Optional.empty();
  @Builder.Default private Optional<String> currentUse = Optional.empty();
  @Builder.Default private Optional<String> zoningClassification = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
}
