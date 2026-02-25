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
public class PropertyOccupancyPeriod {

  private UUID id;
  private String identifier;
  private UUID teamId;
  private UUID propertyId;
  private LocalDate startDate;
  @Builder.Default private Optional<LocalDate> endDate = Optional.empty();
  private OccupancyType type;
  @Builder.Default private Optional<String> occupantName = Optional.empty();
  @Builder.Default private Optional<BigDecimal> monthlyImputedRent = Optional.empty();
  @Builder.Default private Optional<OccupancyEndReason> endReason = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();

  // Audit
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();

  public enum OccupancyType {
    PERSONAL,
    FAMILY,
    BUSINESS
  }

  public enum OccupancyEndReason {
    CONVERTING_TO_RENTAL,
    SELLING,
    RENOVATION,
    OTHER
  }
}
