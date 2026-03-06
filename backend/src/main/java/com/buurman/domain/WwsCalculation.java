package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
public class WwsCalculation {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID propertyId;
  @Builder.Default private Optional<UUID> contractId = Optional.empty();
  private String systemVersion;
  private BigDecimal totalPoints;
  private String sectorClassification;
  @Builder.Default private Optional<BigDecimal> maxRentIndication = Optional.empty();
  private List<CategoryBreakdown> categoryBreakdown;
  private String breakdownJson;
  private String inputDataJson;
  private LocalDate calculationDate;
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();

  public record CategoryBreakdown(
      String key,
      String name,
      String nameNl,
      BigDecimal points,
      String explanation) {}
}
