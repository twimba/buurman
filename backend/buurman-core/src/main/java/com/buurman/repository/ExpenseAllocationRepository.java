package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.EXPENSES;
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
import com.buurman.exception.NotFoundException;
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

  /**
   * Whether any allocation row still references this unit — used to guard unit deletion, since
   * {@code GET /expenses/{id}/allocations} resolves each row's unit by id and would otherwise throw
   * once the unit is soft-deleted.
   */
  public boolean existsByUnitIdAndTeamId(UUID unitId, UUID teamId) {
    return dsl.fetchExists(
        dsl.selectOne()
            .from(EXPENSE_ALLOCATIONS)
            .where(
                EXPENSE_ALLOCATIONS
                    .UNIT_ID
                    .eq(unitId)
                    .and(EXPENSE_ALLOCATIONS.TEAM_ID.eq(teamId))
                    .and(EXPENSE_ALLOCATIONS.DELETED_AT.isNull())));
  }

  /**
   * Retires the expense's current active allocation rows (soft delete) and inserts {@code
   * allocations} as the new active set, for the same expense and team. Call this instead of a bare
   * insert whenever an expense is re-allocated (basis change, manual override, unit-count change) —
   * inserting over still-active rows for the same {@code (expense_id, unit_id)} pair would violate
   * {@code uq_expense_allocations_expense_unit}.
   *
   * <p>The whole method runs in one transaction whose first statement takes a {@code SELECT ... FOR
   * UPDATE} lock on the parent expense row, so two concurrent calls for the same expense serialise.
   * Without that lock, two overlapping calls with disjoint unit sets can both "succeed": neither
   * call's soft-delete conflicts with the other (each either matches nothing, or — under READ
   * COMMITTED — the row it was blocked on no longer matches once unblocked, since the other
   * transaction already soft-deleted it), and both calls' inserts land on different units, so
   * neither ever hits {@code uq_expense_allocations_expense_unit}. The result is two active sets
   * alive at once, silently doubling the expense on a settlement statement. The lock forces the
   * second caller's soft-delete to see the first caller's committed rows as the current active set,
   * so it retires them properly before inserting its own.
   *
   * <p>The lock query also doubles as a tenant check: it filters on {@code team_id}, so a caller
   * passing a {@code teamId} that does not own {@code expenseId} finds no row. Without checking
   * that, the soft-delete below (also team-scoped) would silently match nothing while the insert
   * loop still ran unconditionally -- creating {@code expense_allocations} rows whose {@code
   * expense_id} points at another team's expense while {@code team_id} says otherwise, and leaving
   * that other team's real active allocations untouched but now shadowed by a bogus foreign set.
   */
  public List<ExpenseAllocation> replaceForExpense(
      UUID expenseId, UUID teamId, List<ExpenseAllocation> allocations, UUID actorId) {
    return dsl.transactionResult(
        config -> {
          DSLContext tx = config.dsl();
          LocalDateTime now = LocalDateTime.now(clock);

          UUID lockedExpenseId =
              tx.select(EXPENSES.ID)
                  .from(EXPENSES)
                  .where(EXPENSES.ID.eq(expenseId).and(EXPENSES.TEAM_ID.eq(teamId)))
                  .forUpdate()
                  .fetchOne(EXPENSES.ID);
          if (lockedExpenseId == null) {
            throw new NotFoundException("Expense not found");
          }

          tx.update(EXPENSE_ALLOCATIONS)
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

            tx.insertInto(EXPENSE_ALLOCATIONS)
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
        });
  }
}
