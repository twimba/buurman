package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyInsurance {

  @Schema(description = "Type of insurance coverage for the property")
  public enum InsuranceType {
    BUILDING,
    LIABILITY,
    CONTENTS,
    FLOOD,
    EARTHQUAKE,
    UMBRELLA,
    RENT_GUARANTEE,
    OTHER
  }

  @Schema(description = "Current status of the insurance policy")
  public enum InsuranceStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED
  }

  private UUID id;
  private String identifier;
  private UUID propertyId;
  private UUID teamId;
  private InsuranceType insuranceType;
  @Builder.Default private Optional<String> provider = Optional.empty();
  @Builder.Default private Optional<String> policyNumber = Optional.empty();
  @Builder.Default private Optional<BigDecimal> coverageAmount = Optional.empty();
  @Builder.Default private Optional<String> coverageAmountCurrency = Optional.empty();
  private BigDecimal annualPremium;
  private String annualPremiumCurrency;
  private String paymentFrequency;
  @Builder.Default private Optional<LocalDate> startDate = Optional.empty();
  @Builder.Default private Optional<LocalDate> endDate = Optional.empty();
  private InsuranceStatus status;
  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
