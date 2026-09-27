package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import com.buurman.domain.AllocationBasis;
import com.buurman.domain.ExpenseAllocation;
import com.buurman.exception.NotFoundException;
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
  }

  @Test
  @DisplayName(
      "replaceForExpense with an empty list retires every active row and leaves none — the"
          + " primitive ExpenseAllocationService.retireAllocations uses when a building-level"
          + " expense (BUUR-106 Important 3) is edited to belong to a single unit. Without this,"
          + " the old per-unit rows would stay active alongside the new direct unit charge and"
          + " double-count the expense on a service-charge settlement statement")
  void replaceForExpenseWithEmptyListRetiresEveryActiveRowAndLeavesNone() {
    List<ExpenseAllocation> split =
        List.of(
            allocation(unit1Id, "600.00", AllocationBasis.EQUAL),
            allocation(unit2Id, "600.00", AllocationBasis.EQUAL));
    List<ExpenseAllocation> saved =
        repository.replaceForExpense(expenseId, TEAM_A_ID, split, USER_ID);
    assertThat(saved).hasSize(2);
    List<UUID> savedIds = saved.stream().map(ExpenseAllocation::getId).toList();

    // The expense is now edited to belong to a single unit: retire the building-level split
    // (mirrors ExpenseAllocationService.retireAllocations, which calls exactly this).
    List<ExpenseAllocation> retired =
        repository.replaceForExpense(expenseId, TEAM_A_ID, List.of(), USER_ID);

    assertThat(retired).isEmpty();
    assertThat(repository.findByExpenseIdAndTeamId(expenseId, TEAM_A_ID)).isEmpty();

    int softDeletedRows =
        dsl.fetchCount(
            DSL.selectFrom(DSL.table("expense_allocations"))
                .where(
                    DSL.field("id", UUID.class)
                        .in(savedIds)
                        .and(DSL.field("deleted_at", LocalDateTime.class).isNotNull())));
    assertThat(softDeletedRows).isEqualTo(2);
  }

  @Test
  @DisplayName(
      "two concurrent replaceForExpense calls on the same expense, from two separate connections,"
          + " never leave both sets active at once (BUUR-106 Critical 1 — the row lock on the"
          + " parent expense serialises them so the surviving active rows always sum to exactly"
          + " the expense total, never double)")
  void replaceForExpenseSerialisesConcurrentCallsFromSeparateConnections() throws Exception {
    UUID unit3Id = UUID.randomUUID();
    UUID unit4Id = UUID.randomUUID();
    TestDataHelper.insertUnit(dsl, unit3Id, teamAPropertyId, TEAM_A_ID, "3", "VACANT");
    TestDataHelper.insertUnit(dsl, unit4Id, teamAPropertyId, TEAM_A_ID, "4", "VACANT");

    // Two genuinely separate JDBC connections to the same Postgres testcontainer — not two
    // handles onto one connection — each driving its own ExpenseAllocationRepository instance, so
    // the two replaceForExpense calls below run as two independent database transactions.
    String jdbcUrl = dsl.connectionResult(conn -> conn.getMetaData().getURL());
    DSLContext dslA = DSL.using(pgDataSource(jdbcUrl), SQLDialect.POSTGRES);
    DSLContext dslB = DSL.using(pgDataSource(jdbcUrl), SQLDialect.POSTGRES);
    ExpenseAllocationRepository repositoryA =
        new ExpenseAllocationRepository(
            dslA, TestDataHelper.wireMapper(new ExpenseAllocationRecordMapperImpl()), CLOCK);
    ExpenseAllocationRepository repositoryB =
        new ExpenseAllocationRepository(
            dslB, TestDataHelper.wireMapper(new ExpenseAllocationRecordMapperImpl()), CLOCK);

    // T1 recomputes the whole expense over units 1-3 (400.00 each = 1200.00). T2 concurrently
    // overrides the whole expense onto unit 4 alone (1200.00) — this is the exact reproduction a
    // reviewer ran against a real postgres:18-alpine with the actual partial unique index: before
    // the fix, both committed with no error and the active allocations summed to 2400.00.
    List<ExpenseAllocation> setA =
        List.of(
            allocation(unit1Id, "400.00", AllocationBasis.EQUAL),
            allocation(unit2Id, "400.00", AllocationBasis.EQUAL),
            allocation(unit3Id, "400.00", AllocationBasis.EQUAL));
    List<ExpenseAllocation> setB = List.of(allocation(unit4Id, "1200.00", AllocationBasis.MANUAL));

    CyclicBarrier barrier = new CyclicBarrier(2);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      Future<List<ExpenseAllocation>> futureA =
          executor.submit(
              () -> {
                barrier.await();
                return repositoryA.replaceForExpense(expenseId, TEAM_A_ID, setA, USER_ID);
              });
      Future<List<ExpenseAllocation>> futureB =
          executor.submit(
              () -> {
                barrier.await();
                return repositoryB.replaceForExpense(expenseId, TEAM_A_ID, setB, USER_ID);
              });
      futureA.get(15, TimeUnit.SECONDS);
      futureB.get(15, TimeUnit.SECONDS);
    } finally {
      executor.shutdownNow();
    }

    List<ExpenseAllocation> finalActive = repository.findByExpenseIdAndTeamId(expenseId, TEAM_A_ID);

    BigDecimal sum =
        finalActive.stream()
            .map(a -> a.getAmount().value())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(sum)
        .as("surviving active allocations must sum to exactly the expense total, never double")
        .isEqualByComparingTo("1200.00");

    // The lock forces one call's full set to lose entirely to the other's, never a mix of both —
    // proving the two transactions serialised rather than both independently committing.
    assertThat(finalActive)
        .as("active set must be exactly one caller's full output, not a union of both")
        .satisfiesAnyOf(
            active ->
                assertThat(active)
                    .extracting(ExpenseAllocation::getUnitId)
                    .containsExactlyInAnyOrder(unit1Id, unit2Id, unit3Id),
            active ->
                assertThat(active)
                    .extracting(ExpenseAllocation::getUnitId)
                    .containsExactlyInAnyOrder(unit4Id));
  }

  @Test
  @DisplayName(
      "replaceForExpense called with a foreign teamId is rejected instead of writing an"
          + " allocation row whose expense_id belongs to another team (BUUR-106 follow-up"
          + " register, section F, cross-tenant writes)")
  void replaceForExpenseWithForeignTeamIdIsRejected() {
    List<ExpenseAllocation> teamASet =
        List.of(allocation(unit1Id, "1200.00", AllocationBasis.EQUAL));
    repository.replaceForExpense(expenseId, TEAM_A_ID, teamASet, USER_ID);

    // TEAM_B_ID does not own expenseId. Before the fix, the lock query's team_id filter simply
    // found no row and the method carried on regardless: the team-scoped soft-delete matched
    // nothing (team A's real row stayed active, untouched), but the insert loop ran
    // unconditionally and created a NEW row for unit2Id with expense_id = team A's real expense
    // and team_id = TEAM_B_ID -- a row team B's own scoped reads would then return.
    List<ExpenseAllocation> foreignSet =
        List.of(allocation(unit2Id, "999.00", AllocationBasis.MANUAL));
    assertThatThrownBy(
            () -> repository.replaceForExpense(expenseId, TEAM_B_ID, foreignSet, USER_ID))
        .isInstanceOf(NotFoundException.class);

    // Team A's original allocation is untouched, and team B sees nothing for this expense.
    List<ExpenseAllocation> teamAView = repository.findByExpenseIdAndTeamId(expenseId, TEAM_A_ID);
    assertThat(teamAView).hasSize(1);
    assertThat(teamAView.get(0).getUnitId()).isEqualTo(unit1Id);
    assertThat(repository.findByExpenseIdAndTeamId(expenseId, TEAM_B_ID)).isEmpty();
  }

  private static PGSimpleDataSource pgDataSource(String jdbcUrl) {
    PGSimpleDataSource dataSource = new PGSimpleDataSource();
    dataSource.setUrl(jdbcUrl);
    dataSource.setUser("buurman");
    dataSource.setPassword("buurman");
    return dataSource;
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
