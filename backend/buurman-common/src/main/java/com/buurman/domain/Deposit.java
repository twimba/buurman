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

/** The tenant's deposit for one contract, from expected to held to returned or forfeited. */
@SuppressWarnings("NullAway.Init")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class Deposit {

  public enum DepositStatus {
    EXPECTED,
    HELD,
    PARTIALLY_RETURNED,
    RETURNED,
    FORFEITED
  }

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID contractId;
  @Builder.Default private Optional<UUID> contactId = Optional.empty();
  private MoneyAmount amount;
  @Builder.Default private Optional<LocalDate> receivedDate = Optional.empty();
  @Builder.Default private Optional<String> heldWhere = Optional.empty();
  @Builder.Default private DepositStatus status = DepositStatus.EXPECTED;
  @Builder.Default private Optional<LocalDate> returnDueDate = Optional.empty();
  @Builder.Default private Optional<LocalDate> returnedDate = Optional.empty();

  /** Major units; 0 until something is returned. */
  @Builder.Default private java.math.BigDecimal returnedAmount = java.math.BigDecimal.ZERO;

  @Builder.Default private Optional<String> notes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
