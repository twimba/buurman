package com.buurman.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.buurman.util.MoneyAmount;

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

  public enum InsuranceStatus {
    ACTIVE,
    EXPIRED,
    CANCELLED
  }

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID propertyId;
  private UUID teamId;
  private InsuranceType insuranceType;
  @Builder.Default private Optional<String> provider = Optional.empty();
  @Builder.Default private Optional<String> policyNumber = Optional.empty();
  @Builder.Default private Optional<MoneyAmount> coverageAmount = Optional.empty();
  private MoneyAmount annualPremium;
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
