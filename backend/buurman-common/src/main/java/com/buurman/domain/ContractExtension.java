package com.buurman.domain;

import java.math.BigDecimal;
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
public class ContractExtension {

  public enum ExtensionStatus {
    DRAFT,
    ACTIVE,
    SUPERSEDED,
    CANCELLED,
    DECLINED
  }

  public enum TriggerType {
    MANUAL,
    AUTO
  }

  public enum RentAdjustmentType {
    NONE,
    FIXED_PERCENTAGE,
    FIXED_AMOUNT,
    MANUAL
  }

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID contractId;
  private int extensionNumber;
  private LocalDate previousEndDate;
  @Builder.Default private Optional<LocalDate> newEndDate = Optional.empty();
  private MoneyAmount previousRentAmount;
  private MoneyAmount newRentAmount;
  private RentAdjustmentType rentAdjustmentType;
  @Builder.Default private Optional<BigDecimal> rentAdjustmentValue = Optional.empty();
  private ExtensionStatus status;
  private TriggerType triggerType;
  @Builder.Default private Optional<UUID> rentPeriodId = Optional.empty();
  @Builder.Default private Optional<String> notes = Optional.empty();
  @Builder.Default private Optional<String> declinedReason = Optional.empty();
  @Builder.Default private Optional<Instant> activatedAt = Optional.empty();
  @Builder.Default private Optional<UUID> activatedBy = Optional.empty();
  @Builder.Default private Optional<Instant> confirmedAt = Optional.empty();
  @Builder.Default private Optional<UUID> confirmedBy = Optional.empty();
  @Builder.Default private Optional<Instant> supersededAt = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
