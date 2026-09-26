package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.buurman.domain.AllocationBasis;
import com.buurman.domain.ExpenseAllocation;
import com.buurman.mapper.ExpenseAllocationRecordMapperImpl;
import com.buurman.util.MoneyAmount;

@DisplayName("ExpenseAllocationRepository")
class ExpenseAllocationRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private ExpenseAllocationRepository repository;
  private UUID teamAPropertyId;
  private UUID unit1Id;
  private UUID unit2Id;
  private UUID expenseId;

  @BeforeEach
  void setUpRepository() {
    repository =
        new ExpenseAllocationRepository(
            dsl, TestDataHelper.wireMapper(new ExpenseAllocationRecordMapperImpl()), CLOCK);

    teamAPropertyId = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    unit1Id = UUID.randomUUID();
    unit2Id = UUID.randomUUID();
    TestDataHelper.insertUnit(dsl, unit1Id, teamAPropertyId, TEAM_A_ID, "1", "VACANT");
    TestDataHelper.insertUnit(dsl, unit2Id, teamAPropertyId, TEAM_A_ID, "2", "VACANT");
    expenseId =
        TestDataHelper.insertExpense(
            dsl, teamAPropertyId, TEAM_A_ID, USER_ID, new BigDecimal("120000"));
  }

  @Test
  @DisplayName(
      "replaceForExpense called twice leaves exactly one active row per unit, soft-deletes the "
          + "first set instead of removing it, and a team-B-scoped read sees none of it")
  void replaceForExpenseRetiresPreviousSetAndIsolatesByTeam() {
    List<ExpenseAllocation> firstSet =
        List.of(
            allocation(unit1Id, "600.00", AllocationBasis.EQUAL),
            allocation(unit2Id, "600.00", AllocationBasis.EQUAL));

    List<ExpenseAllocation> savedFirst =
        repository.replaceForExpense(expenseId, TEAM_A_ID, firstSet, USER_ID);
    assertThat(savedFirst).hasSize(2);
    assertThat(savedFirst).allSatisfy(a -> assertThat(a.getIdentifier()).isPresent());

    List<UUID> firstSetIds = savedFirst.stream().map(ExpenseAllocation::getId).toList();

    List<ExpenseAllocation> activeAfterFirst =
        repository.findByExpenseIdAndTeamId(expenseId, TEAM_A_ID);
    assertThat(activeAfterFirst).hasSize(2);
    assertThat(activeAfterFirst)
        .extracting(a -> a.getAmount().value().toPlainString())
        .containsExactlyInAnyOrder("600.00", "600.00");

    // Re-allocate with a different split for the same two units.
    List<ExpenseAllocation> secondSet =
        List.of(
            allocation(unit1Id, "900.00", AllocationBasis.AREA),
            allocation(unit2Id, "300.00", AllocationBasis.AREA));

    List<ExpenseAllocation> savedSecond =
        repository.replaceForExpense(expenseId, TEAM_A_ID, secondSet, USER_ID);
    assertThat(savedSecond).hasSize(2);

    // Exactly one active row per unit — the second set.
    List<ExpenseAllocation> activeAfterSecond =
        repository.findByExpenseIdAndTeamId(expenseId, TEAM_A_ID);
    assertThat(activeAfterSecond).hasSize(2);
    assertThat(activeAfterSecond)
        .extracting(ExpenseAllocation::getUnitId)
        .containsExactlyInAnyOrder(unit1Id, unit2Id);
    assertThat(activeAfterSecond)
        .extracting(a -> a.getAmount().value().toPlainString())
        .containsExactlyInAnyOrder("900.00", "300.00");
    assertThat(activeAfterSecond)
        .extracting(ExpenseAllocation::getBasis)
        .containsOnly(AllocationBasis.AREA);

    // The first set is soft-deleted, not removed.
    int softDeletedFirstSetRows =
        dsl.fetchCount(
            DSL.selectFrom(DSL.table("expense_allocations"))
                .where(
                    DSL.field("id", UUID.class)
                        .in(firstSetIds)
                        .and(DSL.field("deleted_at", LocalDateTime.class).isNotNull())));
    assertThat(softDeletedFirstSetRows).isEqualTo(2);

    // A team-B-scoped read sees none of team A's allocation rows.
    assertThat(repository.findByExpenseIdAndTeamId(expenseId, TEAM_B_ID)).isEmpty();
    assertThat(repository.findByUnitIdAndTeamId(unit1Id, TEAM_B_ID)).isEmpty();
    assertThat(repository.findByUnitIdAndTeamId(unit1Id, TEAM_A_ID)).hasSize(1);
  }

  private ExpenseAllocation allocation(UUID unitId, String amount, AllocationBasis basis) {
    return ExpenseAllocation.builder()
        .unitId(unitId)
        .teamId(TEAM_A_ID)
        .amount(MoneyAmount.of(new BigDecimal(amount), "EUR"))
        .basis(basis)
        .build();
  }
}
