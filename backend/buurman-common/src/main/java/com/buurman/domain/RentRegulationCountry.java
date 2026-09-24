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
public class RentRegulationCountry {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private String countryCode;
  private String countryName;
  private boolean hasRegionalRegulations;
  @Builder.Default private Optional<String> summary = Optional.empty();
  @Builder.Default private Optional<Instant> lastReviewedAt = Optional.empty();
  @Builder.Default private LateFeePolicy lateFeePolicy = LateFeePolicy.UNKNOWN;
  @Builder.Default private Optional<BigDecimal> lateFeeMaxPercentage = Optional.empty();
  @Builder.Default private Optional<String> lateFeeNotes = Optional.empty();

  /** Default days a tenant gets to settle after a formal notice of overdue rent. */
  @Builder.Default private Optional<Integer> formalNoticeDays = Optional.empty();

  private Instant createdAt;
  private Instant updatedAt;
  @Builder.Default private Optional<String> createdBy = Optional.empty();
  @Builder.Default private Optional<String> updatedBy = Optional.empty();
}
