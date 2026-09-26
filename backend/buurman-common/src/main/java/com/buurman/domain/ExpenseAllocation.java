package com.buurman.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import com.buurman.util.MoneyAmount;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One unit's stored share of a building-level expense. */
@SuppressWarnings("NullAway.Init")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseAllocation {

  private UUID id;
  @Builder.Default private Optional<Sid> identifier = Optional.empty();
  private UUID teamId;
  private UUID expenseId;
  private UUID unitId;
  private MoneyAmount amount;
  private AllocationBasis basis;

  @Builder.Default private Optional<Instant> createdAt = Optional.empty();
  @Builder.Default private Optional<Instant> updatedAt = Optional.empty();
  @Builder.Default private Optional<UUID> createdBy = Optional.empty();
  @Builder.Default private Optional<UUID> updatedBy = Optional.empty();
  @Builder.Default private Optional<Instant> deletedAt = Optional.empty();
}
