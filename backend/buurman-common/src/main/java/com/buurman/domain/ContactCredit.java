package com.buurman.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.buurman.util.MoneyAmount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Money the landlord owes a tenant: an overpayment or a manual credit note. It is consumed by
 * applying it to later payments (as CREDIT receivals) or closed by refunding the remainder.
 */
@SuppressWarnings("NullAway.Init")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class ContactCredit {

  public enum CreditSource {
    OVERPAYMENT,
    CREDIT_NOTE
  }

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID contactId;
  @Builder.Default private Optional<UUID> contractId = Optional.empty();
  private MoneyAmount amount;

  /** Unapplied part of {@code amount}, in major units. */
  private BigDecimal remainingAmount;

  private CreditSource source;
  @Builder.Default private Optional<String> reason = Optional.empty();
  @Builder.Default private Optional<UUID> sourcePaymentId = Optional.empty();

  /** The receival whose excess created this credit; reversing it reverses the credit. */
  @Builder.Default private Optional<UUID> sourceReceivalId = Optional.empty();

  @Builder.Default private Optional<Instant> refundedAt = Optional.empty();
  @Builder.Default private Optional<String> refundNotes = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
