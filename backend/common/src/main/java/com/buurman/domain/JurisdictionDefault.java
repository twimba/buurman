package com.buurman.domain;

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
public class JurisdictionDefault {

  private UUID id;
  private String countryCode;
  @Builder.Default private Optional<String> regionCode = Optional.empty();
  @Builder.Default private Optional<String> landlordType = Optional.empty();
  @Builder.Default private Optional<Boolean> furnished = Optional.empty();
  private String fieldName;
  private String value;
  private LocalDate validFrom;
  @Builder.Default private Optional<LocalDate> validUntil = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
}
