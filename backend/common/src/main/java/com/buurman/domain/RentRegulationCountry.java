package com.buurman.domain;

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
  private Instant createdAt;
  private Instant updatedAt;
  @Builder.Default private Optional<String> createdBy = Optional.empty();
  @Builder.Default private Optional<String> updatedBy = Optional.empty();
}
