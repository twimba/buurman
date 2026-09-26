package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.EXPENSE_ALLOCATIONS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.ExpenseAllocation;
import com.buurman.mapper.ExpenseAllocationRecordMapper;
import com.buurman.util.SidGenerator;

import lombok.RequiredArgsConstructor;

/**
 * Team-scoped persistence for {@link ExpenseAllocation} rows. Every query filters {@code team_id}.
 * Rows are never hard-deleted: {@link #replaceForExpense} soft-deletes the previous set before
 * inserting the new one, so {@code uq_expense_allocations_expense_unit} (unique on {@code
 * (expense_id, unit_id)} while {@code deleted_at IS NULL}) never trips on a re-allocation, and the
 * prior allocation stays available for audit history.
 */
@Repository
@RequiredArgsConstructor
public class ExpenseAllocationRepository {

  private final DSLContext dsl;
  private final ExpenseAllocationRecordMapper mapper;
  private final Clock clock;

  public List<ExpenseAllocation> findByExpenseIdAndTeamId(UUID expenseId, UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(EXPENSE_ALLOCATIONS)
            .where(
                EXPENSE_ALLOCATIONS
                    .EXPENSE_ID
                    .eq(expenseId)
                    .and(EXPENSE_ALLOCATIONS.TEAM_ID.eq(teamId))
                    .and(EXPENSE_ALLOCATIONS.DELETED_AT.isNull()))
            .fetch()
            .map(mapper::toDomain));
  }

  public List<ExpenseAllocation> findByUnitIdAndTeamId(UUID unitId, UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(EXPENSE_ALLOCATIONS)
            .where(
                EXPENSE_ALLOCATIONS
                    .UNIT_ID
                    .eq(unitId)
                    .and(EXPENSE_ALLOCATIONS.TEAM_ID.eq(teamId))
                    .and(EXPENSE_ALLOCATIONS.DELETED_AT.isNull()))
            .fetch()
            .map(mapper::toDomain));
  }

  /**
   * Retires the expense's current active allocation rows (soft delete) and inserts {@code
   * allocations} as the new active set, for the same expense and team. Call this instead of a bare
   * insert whenever an expense is re-allocated (basis change, manual override, unit-count change) —
   * inserting over still-active rows for the same {@code (expense_id, unit_id)} pair would violate
   * {@code uq_expense_allocations_expense_unit}.
   */
  public List<ExpenseAllocation> replaceForExpense(
      UUID expenseId, UUID teamId, List<ExpenseAllocation> allocations, UUID actorId) {
    LocalDateTime now = LocalDateTime.now(clock);

    dsl.update(EXPENSE_ALLOCATIONS)
        .set(EXPENSE_ALLOCATIONS.DELETED_AT, now)
        .set(EXPENSE_ALLOCATIONS.UPDATED_BY, actorId)
        .set(EXPENSE_ALLOCATIONS.UPDATED_AT, now)
        .where(
            EXPENSE_ALLOCATIONS
                .EXPENSE_ID
                .eq(expenseId)
                .and(EXPENSE_ALLOCATIONS.TEAM_ID.eq(teamId))
                .and(EXPENSE_ALLOCATIONS.DELETED_AT.isNull()))
        .execute();

    List<ExpenseAllocation> saved = new ArrayList<>();
    for (ExpenseAllocation allocation : allocations) {
      UUID id = UUID.randomUUID();
      var identifier = SidGenerator.newExpenseAllocationId();

      dsl.insertInto(EXPENSE_ALLOCATIONS)
          .set(EXPENSE_ALLOCATIONS.ID, id)
          .set(EXPENSE_ALLOCATIONS.IDENTIFIER, identifier)
          .set(EXPENSE_ALLOCATIONS.TEAM_ID, teamId)
          .set(EXPENSE_ALLOCATIONS.EXPENSE_ID, expenseId)
          .set(EXPENSE_ALLOCATIONS.UNIT_ID, allocation.getUnitId())
          .set(EXPENSE_ALLOCATIONS.AMOUNT, allocation.getAmount().value())
          .set(EXPENSE_ALLOCATIONS.AMOUNT_CURRENCY, allocation.getAmount().currency())
          .set(EXPENSE_ALLOCATIONS.BASIS, allocation.getBasis().name())
          .set(EXPENSE_ALLOCATIONS.CREATED_AT, now)
          .set(EXPENSE_ALLOCATIONS.UPDATED_AT, now)
          .set(EXPENSE_ALLOCATIONS.CREATED_BY, actorId)
          .set(EXPENSE_ALLOCATIONS.UPDATED_BY, actorId)
          .execute();

      allocation.setId(id);
      allocation.setTeamId(teamId);
      allocation.setExpenseId(expenseId);
      allocation.setIdentifier(Optional.of(identifier));
      allocation.setCreatedAt(Optional.of(now.toInstant(UTC)));
      allocation.setUpdatedAt(Optional.of(now.toInstant(UTC)));
      allocation.setCreatedBy(Optional.of(actorId));
      allocation.setUpdatedBy(Optional.of(actorId));
      saved.add(allocation);
    }

    return List.copyOf(saved);
  }
}
