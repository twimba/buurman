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
public class RentRegulationRegion {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID countryId;
  private String regionCode;
  private String regionName;
  @Builder.Default private Optional<String> summary = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  @Builder.Default private Optional<String> createdBy = Optional.empty();
  @Builder.Default private Optional<String> updatedBy = Optional.empty();
}
