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

/**
 * An agreed schedule to pay off an overdue balance in instalments. The covered payments are settled
 * with PLAN receivals when the plan starts; INSTALMENT payments carry the debt.
 */
@SuppressWarnings("NullAway.Init")
@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class PaymentPlan {

  public enum Frequency {
    WEEKLY,
    BIWEEKLY,
    MONTHLY
  }

  public enum PlanStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
  }

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID contractId;
  @Builder.Default private Optional<UUID> contactId = Optional.empty();
  private MoneyAmount totalAmount;
  private int instalmentCount;
  private LocalDate startDate;
  @Builder.Default private Frequency frequency = Frequency.MONTHLY;
  @Builder.Default private PlanStatus status = PlanStatus.ACTIVE;
  @Builder.Default private Optional<String> notes = Optional.empty();
  @Builder.Default private Optional<String> cancelReason = Optional.empty();
  private Instant createdAt;
  private Instant updatedAt;
  private UUID createdBy;
  private UUID updatedBy;
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
