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
public class RentRegulationRule {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID countryId;
  @Builder.Default private Optional<UUID> regionId = Optional.empty();
  private int year;
  private String propertyCategory;
  @Builder.Default private Optional<BigDecimal> maxIncreasePercentage = Optional.empty();
  private MaxIncreaseType maxIncreaseType;
  @Builder.Default private Optional<String> indexName = Optional.empty();
  @Builder.Default private Optional<BigDecimal> indexValue = Optional.empty();
  @Builder.Default private Optional<LocalDate> effectiveDate = Optional.empty();
  @Builder.Default private Optional<Integer> noticePeriodDays = Optional.empty();
  private RentFrequency frequency;
  @Builder.Default private Optional<String> additionalConditions = Optional.empty();
  @Builder.Default private Optional<String> sourceUrl = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
  // BUUR-93: typed dimensional columns (V055). NULL = wildcard "applies to all".
  @Builder.Default private Optional<String> regime = Optional.empty();
  @Builder.Default private Optional<String> propertyType = Optional.empty();
  @Builder.Default private Optional<String> contractType = Optional.empty();
  @Builder.Default private Optional<String> taxRegime = Optional.empty();
  @Builder.Default private Optional<String> tenancyPhase = Optional.empty();
  @Builder.Default private Optional<Integer> buildYearMin = Optional.empty();
  @Builder.Default private Optional<Integer> buildYearMax = Optional.empty();
  @Builder.Default private Optional<String> epcClassMin = Optional.empty();
  @Builder.Default private Optional<String> epcClassMax = Optional.empty();
  @Builder.Default private Optional<LocalDate> contractSignedAfter = Optional.empty();
  @Builder.Default private Optional<LocalDate> contractSignedBefore = Optional.empty();
  @Builder.Default private Optional<Integer> landlordMinProperties = Optional.empty();
  @Builder.Default private Optional<String> areaCode = Optional.empty();

  private Instant createdAt;
  private Instant updatedAt;
  @Builder.Default private Optional<String> createdBy = Optional.empty();
  @Builder.Default private Optional<String> updatedBy = Optional.empty();
}
