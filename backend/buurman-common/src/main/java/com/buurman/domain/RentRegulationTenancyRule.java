package com.buurman.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A display-only tenancy-law reference entry for a regulation country. */
@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentRegulationTenancyRule {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID countryId;
  @Builder.Default private Optional<UUID> regionId = Optional.empty();
  private TenancyRuleTopic topic;
  private String label;
  private String value;
  @Builder.Default private Optional<LocalDate> effectiveFrom = Optional.empty();
  @Builder.Default private Optional<String> legalBasis = Optional.empty();
  @Builder.Default private Optional<String> sourceUrl = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  @Builder.Default private Optional<String> createdBy = Optional.empty();
  @Builder.Default private Optional<String> updatedBy = Optional.empty();
}
