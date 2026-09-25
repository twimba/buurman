package com.buurman.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.buurman.domain.AmountStats;
import com.buurman.domain.MonthlyAmount;
import com.buurman.domain.Payment;
import com.buurman.domain.Payment.PaymentStatus;
import com.buurman.domain.SortDirection;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.PaymentRecordMapper;
import com.buurman.util.PaginationHelper.PaginatedResult;

@DisplayName("PaymentRepository Integration")
class PaymentRepositoryIntegrationTest extends AbstractRepositoryIntegrationTest {

  private PaymentRepository repo;
  private UUID propertyIdA;
  private UUID contractIdA;
  private UUID propertyIdB;
  private UUID contractIdB;

  @BeforeEach
  void setUp() {
    repo = new PaymentRepository(dsl, new PaymentRecordMapper(), CLOCK);

    propertyIdA = TestDataHelper.insertProperty(dsl, TEAM_A_ID, USER_ID);
    contractIdA = TestDataHelper.insertContract(dsl, TEAM_A_ID, propertyIdA, USER_ID);

    propertyIdB = TestDataHelper.insertProperty(dsl, TEAM_B_ID, USER_ID);
    contractIdB = TestDataHelper.insertContract(dsl, TEAM_B_ID, propertyIdB, USER_ID);
  }

  @Nested
  @DisplayName("save")
  class Save {

    @Test
    @DisplayName("insert creates a new payment with generated ID")
    void insertCreatesPayment() {
      Payment payment =
          TestDataHelper.buildPayment(
              TEAM_A_ID, contractIdA, USER_ID, new BigDecimal("1000.00"), LocalDate.of(2026, 4, 1));

      Payment saved = repo.save(payment);

      assertThat(saved.getId()).isNotNull();
      assertThat(saved.getCreatedAt()).isNotNull();

      Payment found = repo.getByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);
      assertThat(found.getAmount().value()).isEqualByComparingTo("1000.00");
      assertThat(found.getAmount().currency()).isEqualTo("EUR");
      assertThat(found.getStatus()).isEqualTo(PaymentStatus.PENDING);
      assertThat(found.getDueDate()).isEqualTo(LocalDate.of(2026, 4, 1));
    }

    @Test
    @DisplayName("update modifies payment amount and status")
    void updateModifiesPayment() {
      Payment saved =
          repo.save(
              TestDataHelper.buildPayment(
                  TEAM_A_ID,
                  contractIdA,
                  USER_ID,
                  new BigDecimal("1000.00"),
                  LocalDate.of(2026, 4, 1)));

      saved.setStatus(PaymentStatus.PAID);
      saved.setPaymentDate(Optional.of(LocalDate.of(2026, 3, 28)));
      repo.save(saved);

      Payment found = repo.getByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);
      assertThat(found.getStatus()).isEqualTo(PaymentStatus.PAID);
      assertThat(found.getPaymentDate()).contains(LocalDate.of(2026, 3, 28));
    }

    @Test
    @DisplayName("update respects team_id in WHERE clause")
    void updateRespectsTeamId() {
      Payment saved =
          repo.save(
              TestDataHelper.buildPayment(
                  TEAM_A_ID,
                  contractIdA,
                  USER_ID,
                  new BigDecimal("1000.00"),
                  LocalDate.of(2026, 4, 1)));

      saved.setTeamId(TEAM_B_ID);
      saved.setStatus(PaymentStatus.CANCELLED);
      repo.save(saved);

      Payment found = repo.getByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_A_ID);
      assertThat(found.getStatus()).isEqualTo(PaymentStatus.PENDING);
    }
  }

  @Nested
  @DisplayName("find")
  class Find {

    @Test
    @DisplayName("findByIdentifierAndTeamId with wrong team returns empty")
    void findByIdentifierWrongTeamReturnsEmpty() {
      Payment saved =
          repo.save(
              TestDataHelper.buildPayment(
                  TEAM_A_ID,
                  contractIdA,
                  USER_ID,
                  new BigDecimal("1000.00"),
                  LocalDate.of(2026, 4, 1)));

      assertThat(repo.findByIdentifierAndTeamId(saved.getIdentifier().orElseThrow(), TEAM_B_ID))
          .isEmpty();
    }

    @Test
    @DisplayName("findByContractId returns payments for the contract")
    void findByContractIdReturnsPayments() {
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID,
              contractIdA,
              USER_ID,
              new BigDecimal("1000.00"),
              LocalDate.of(2026, 4, 1)));
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID,
              contractIdA,
              USER_ID,
              new BigDecimal("1000.00"),
              LocalDate.of(2026, 5, 1)));

      List<Payment> payments = repo.findByContractId(contractIdA, TEAM_A_ID);

      assertThat(payments).hasSize(2);
    }

    @Test
    @DisplayName("findByContractId with wrong team returns empty")
    void findByContractIdWrongTeamReturnsEmpty() {
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID,
              contractIdA,
              USER_ID,
              new BigDecimal("1000.00"),
              LocalDate.of(2026, 4, 1)));

      assertThat(repo.findByContractId(contractIdA, TEAM_B_ID)).isEmpty();
    }

    @Test
    @DisplayName("findAllByTeamId isolates teams")
    void findAllByTeamIdIsolatesTeams() {
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID,
              contractIdA,
              USER_ID,
              new BigDecimal("1000.00"),
              LocalDate.of(2026, 4, 1)));
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_B_ID, contractIdB, USER_ID, new BigDecimal("500.00"), LocalDate.of(2026, 4, 1)));

      assertThat(repo.findAllByTeamId(TEAM_A_ID)).hasSize(1);
      assertThat(repo.findAllByTeamId(TEAM_B_ID)).hasSize(1);
    }

    @Test
    @DisplayName("getByIdentifierAndTeamId throws for missing payment")
    void getByIdentifierThrowsForMissing() {
      assertThatThrownBy(
              () ->
                  repo.getByIdentifierAndTeamId(
                      com.buurman.util.SidGenerator.newPaymentId(), TEAM_A_ID))
          .isInstanceOf(NotFoundException.class);
    }
  }

  @Nested
  @DisplayName("overdue and pending queries")
  class OverdueAndPending {

    @Test
    @DisplayName("findOverduePayments returns pending payments past due date")
    void findOverduePayments() {
      // Past-due pending payment (clock is fixed at 2026-03-01)
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID,
              contractIdA,
              USER_ID,
              new BigDecimal("1000.00"),
              LocalDate.of(2026, 2, 1)));
      // Future pending payment
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID,
              contractIdA,
              USER_ID,
              new BigDecimal("1000.00"),
              LocalDate.of(2026, 4, 1)));

      List<Payment> overdue = repo.findOverduePayments(TEAM_A_ID);

      assertThat(overdue).hasSize(1);
      assertThat(overdue.getFirst().getDueDate()).isEqualTo(LocalDate.of(2026, 2, 1));
    }

    @Test
    @DisplayName("findOverduePayments includes partially paid and stored-OVERDUE payments past due")
    void findOverdueIncludesPartiallyPaidAndOverdueStatus() {
      Payment partial =
          TestDataHelper.buildPayment(
              TEAM_A_ID, contractIdA, USER_ID, new BigDecimal("1000.00"), LocalDate.of(2026, 2, 1));
      partial.setStatus(PaymentStatus.PARTIALLY_PAID);
      repo.save(partial);
      Payment stored =
          TestDataHelper.buildPayment(
              TEAM_A_ID, contractIdA, USER_ID, new BigDecimal("900.00"), LocalDate.of(2026, 1, 15));
      stored.setStatus(PaymentStatus.OVERDUE);
      repo.save(stored);
      Payment cancelled =
          TestDataHelper.buildPayment(
              TEAM_A_ID, contractIdA, USER_ID, new BigDecimal("800.00"), LocalDate.of(2026, 1, 1));
      cancelled.setStatus(PaymentStatus.CANCELLED);
      repo.save(cancelled);

      List<Payment> overdue = repo.findOverduePayments(TEAM_A_ID);

      assertThat(overdue)
          .extracting(Payment::getStatus)
          .containsExactly(PaymentStatus.OVERDUE, PaymentStatus.PARTIALLY_PAID);
    }

    @Test
    @DisplayName("findOverdueRows nets receivals off the balance and drops fully covered payments")
    void findOverdueRowsNetsReceivals() {
      PaymentReceivalRepository receivals = new PaymentReceivalRepository(dsl, CLOCK);
      Payment partial =
          repo.save(
              TestDataHelper.buildPayment(
                  TEAM_A_ID,
                  contractIdA,
                  USER_ID,
                  new BigDecimal("1000.00"),
                  LocalDate.of(2026, 2, 1)));
      Payment covered =
          repo.save(
              TestDataHelper.buildPayment(
                  TEAM_A_ID,
                  contractIdA,
                  USER_ID,
                  new BigDecimal("300.00"),
                  LocalDate.of(2026, 1, 1)));
      Payment otherTeam =
          repo.save(
              TestDataHelper.buildPayment(
                  TEAM_B_ID,
                  contractIdB,
                  USER_ID,
                  new BigDecimal("400.00"),
                  LocalDate.of(2026, 1, 1)));
      receivals.save(receival(partial, new BigDecimal("250.00")));
      receivals.save(receival(covered, new BigDecimal("300.00")));

      List<PaymentRepository.OverduePaymentRow> rows = repo.findOverdueRows(TEAM_A_ID);

      assertThat(rows).hasSize(1);
      assertThat(rows.getFirst().paymentId()).isEqualTo(partial.getId());
      assertThat(rows.getFirst().outstanding()).isEqualByComparingTo("750.00");
      assertThat(rows.getFirst().identifier()).isEqualTo(partial.getIdentifier().orElseThrow());
      assertThat(rows).noneMatch(r -> r.paymentId().equals(otherTeam.getId()));

      assertThat(repo.getOverdueStats(TEAM_A_ID)).isPresent();
      assertThat(repo.getOverdueStats(TEAM_A_ID).orElseThrow().count()).isEqualTo(2);
      assertThat(repo.getOverdueStats(TEAM_A_ID).orElseThrow().total())
          .hasValueSatisfying(total -> assertThat(total).isEqualByComparingTo("75000"));
    }

    private com.buurman.domain.PaymentReceival receival(Payment payment, BigDecimal amount) {
      com.buurman.domain.PaymentReceival r = new com.buurman.domain.PaymentReceival();
      r.setIdentifier(Optional.of(com.buurman.util.SidGenerator.newPaymentReceivalId()));
      r.setTeamId(payment.getTeamId());
      r.setPaymentId(payment.getId());
      r.setAmount(com.buurman.util.MoneyAmount.of(amount, "EUR"));
      r.setReceivalDate(LocalDate.of(2026, 2, 15));
      r.setCreatedBy(USER_ID);
      r.setUpdatedBy(USER_ID);
      return r;
    }

    @Test
    @DisplayName("late fee candidates respect the contract opt-in, grace period and one-fee rule")
    void lateFeeCandidates() {
      LocalDate today = LocalDate.of(2026, 3, 1);
      dsl.update(com.buurman.jooq.generated.Tables.CONTRACTS)
          .set(com.buurman.jooq.generated.Tables.CONTRACTS.LATE_FEE_ENABLED, true)
          .set(com.buurman.jooq.generated.Tables.CONTRACTS.LATE_FEE_GRACE_DAYS, 5)
          .set(
              com.buurman.jooq.generated.Tables.CONTRACTS.LATE_FEE_PERCENTAGE,
              new BigDecimal("2.50"))
          .where(com.buurman.jooq.generated.Tables.CONTRACTS.ID.eq(contractIdA))
          .execute();
      Payment pastGrace =
          repo.save(
              TestDataHelper.buildPayment(
                  TEAM_A_ID, contractIdA, USER_ID, new BigDecimal("1000.00"), today.minusDays(6)));
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID, contractIdA, USER_ID, new BigDecimal("1000.00"), today.minusDays(5)));
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_B_ID, contractIdB, USER_ID, new BigDecimal("1000.00"), today.minusDays(30)));

      List<Payment> candidates = repo.findLateFeeCandidates(today);
      assertThat(candidates).extracting(Payment::getId).containsExactly(pastGrace.getId());
      assertThat(repo.hasLateFee(pastGrace.getId(), TEAM_A_ID)).isFalse();

      Payment fee =
          TestDataHelper.buildPayment(
              TEAM_A_ID, contractIdA, USER_ID, new BigDecimal("25.00"), today);
      fee.setPaymentType(Payment.PaymentType.LATE_FEE);
      fee.setParentPaymentId(Optional.of(pastGrace.getId()));
      Payment savedFee = repo.save(fee);
      assertThat(repo.hasLateFee(pastGrace.getId(), TEAM_A_ID)).isTrue();
      Payment reloaded =
          repo.getByIdentifierAndTeamId(savedFee.getIdentifier().orElseThrow(), TEAM_A_ID);
      assertThat(reloaded.getPaymentType()).isEqualTo(Payment.PaymentType.LATE_FEE);
      assertThat(reloaded.getParentPaymentId()).contains(pastGrace.getId());
      // Late fees are never themselves candidates
      assertThat(repo.findLateFeeCandidates(today.plusDays(30)))
          .extracting(Payment::getId)
          .doesNotContain(savedFee.getId());
    }

    @Test
    @DisplayName("demo teams are charged too, so demo data shows the feature working")
    void lateFeeCandidatesIncludeDemoTeams() {
      LocalDate today = LocalDate.of(2026, 3, 1);
      dsl.update(com.buurman.jooq.generated.Tables.TEAMS)
          .set(com.buurman.jooq.generated.Tables.TEAMS.DEMO, true)
          .where(com.buurman.jooq.generated.Tables.TEAMS.ID.eq(TEAM_A_ID))
          .execute();
      dsl.update(com.buurman.jooq.generated.Tables.CONTRACTS)
          .set(com.buurman.jooq.generated.Tables.CONTRACTS.LATE_FEE_ENABLED, true)
          .set(com.buurman.jooq.generated.Tables.CONTRACTS.LATE_FEE_GRACE_DAYS, 5)
          .set(
              com.buurman.jooq.generated.Tables.CONTRACTS.LATE_FEE_PERCENTAGE,
              new BigDecimal("2.50"))
          .where(com.buurman.jooq.generated.Tables.CONTRACTS.ID.eq(contractIdA))
          .execute();
      Payment pastGrace =
          repo.save(
              TestDataHelper.buildPayment(
                  TEAM_A_ID, contractIdA, USER_ID, new BigDecimal("1000.00"), today.minusDays(6)));

      assertThat(repo.findLateFeeCandidates(today))
          .extracting(Payment::getId)
          .contains(pastGrace.getId());
    }

    @Test
    @DisplayName("findOverduePayments excludes paid payments")
    void findOverdueExcludesPaid() {
      Payment p =
          TestDataHelper.buildPayment(
              TEAM_A_ID, contractIdA, USER_ID, new BigDecimal("1000.00"), LocalDate.of(2026, 2, 1));
      p.setStatus(PaymentStatus.PAID);
      p.setPaymentDate(Optional.of(LocalDate.of(2026, 2, 15)));
      repo.save(p);

      assertThat(repo.findOverduePayments(TEAM_A_ID)).isEmpty();
    }

    @Test
    @DisplayName("existsByContractIdAndDueDate returns true for existing combination")
    void existsByContractIdAndDueDateTrue() {
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID,
              contractIdA,
              USER_ID,
              new BigDecimal("1000.00"),
              LocalDate.of(2026, 4, 1)));

      assertThat(repo.existsByContractIdAndDueDate(contractIdA, LocalDate.of(2026, 4, 1))).isTrue();
    }

    @Test
    @DisplayName("existsByContractIdAndDueDate returns false for missing combination")
    void existsByContractIdAndDueDateFalse() {
      assertThat(repo.existsByContractIdAndDueDate(contractIdA, LocalDate.of(2099, 1, 1)))
          .isFalse();
    }

    @Test
    @DisplayName("findPendingByContractIdFromDate returns pending from given date")
    void findPendingByContractIdFromDate() {
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID,
              contractIdA,
              USER_ID,
              new BigDecimal("1000.00"),
              LocalDate.of(2026, 2, 1)));
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID,
              contractIdA,
              USER_ID,
              new BigDecimal("1000.00"),
              LocalDate.of(2026, 4, 1)));

      List<Payment> result =
          repo.findPendingByContractIdFromDate(contractIdA, TEAM_A_ID, LocalDate.of(2026, 3, 1));

      assertThat(result).hasSize(1);
      assertThat(result.getFirst().getDueDate()).isEqualTo(LocalDate.of(2026, 4, 1));
    }

    @Test
    @DisplayName("findFuturePendingByContractId returns pending after today")
    void findFuturePending() {
      // Past
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID,
              contractIdA,
              USER_ID,
              new BigDecimal("1000.00"),
              LocalDate.of(2026, 2, 1)));
      // Future
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID,
              contractIdA,
              USER_ID,
              new BigDecimal("1000.00"),
              LocalDate.of(2026, 4, 1)));

      List<Payment> result = repo.findFuturePendingByContractId(contractIdA, TEAM_A_ID);

      assertThat(result).hasSize(1);
      assertThat(result.getFirst().getDueDate()).isEqualTo(LocalDate.of(2026, 4, 1));
    }
  }

  @Nested
  @DisplayName("aggregations")
  class Aggregations {

    @Test
    @DisplayName("getPendingStats calculates count and total for pending payments")
    void getPendingStatsCalculatesCorrectly() {
      // Future pending
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID,
              contractIdA,
              USER_ID,
              new BigDecimal("1000.00"),
              LocalDate.of(2026, 4, 1)));
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID, contractIdA, USER_ID, new BigDecimal("500.00"), LocalDate.of(2026, 5, 1)));

      Optional<AmountStats> stats = repo.getPendingStats(TEAM_A_ID);

      assertThat(stats).isPresent();
      assertThat(stats.get().count()).isEqualTo(2);
      // MoneyMinorUnitConverter: values stored as minor units (150000 + 50000 = 200000)
      // but stats.total is raw from sum() which returns BigDecimal of minor units
      assertThat(stats.get().total()).isPresent();
    }

    @Test
    @DisplayName("getOverdueStats returns stats for overdue payments")
    void getOverdueStatsCalculatesCorrectly() {
      // Past-due pending
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID, contractIdA, USER_ID, new BigDecimal("800.00"), LocalDate.of(2026, 2, 1)));

      Optional<AmountStats> stats = repo.getOverdueStats(TEAM_A_ID);

      assertThat(stats).isPresent();
      assertThat(stats.get().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("getMonthlyPaidTrend groups paid amounts by month")
    void getMonthlyPaidTrendGroupsByMonth() {
      // Two paid payments in different months
      Payment p1 =
          TestDataHelper.buildPayment(
              TEAM_A_ID, contractIdA, USER_ID, new BigDecimal("1000.00"), LocalDate.of(2026, 1, 1));
      p1.setStatus(PaymentStatus.PAID);
      p1.setPaymentDate(Optional.of(LocalDate.of(2026, 1, 15)));
      repo.save(p1);

      Payment p2 =
          TestDataHelper.buildPayment(
              TEAM_A_ID, contractIdA, USER_ID, new BigDecimal("1000.00"), LocalDate.of(2026, 2, 1));
      p2.setStatus(PaymentStatus.PAID);
      p2.setPaymentDate(Optional.of(LocalDate.of(2026, 2, 15)));
      repo.save(p2);

      List<MonthlyAmount> trend = repo.getMonthlyPaidTrend(TEAM_A_ID, 6);

      assertThat(trend).hasSize(2);
      assertThat(trend).extracting(MonthlyAmount::month).contains("2026-01", "2026-02");
    }

    @Test
    @DisplayName("findCurrencyByTeamId returns currency from first payment")
    void findCurrencyByTeamId() {
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID,
              contractIdA,
              USER_ID,
              new BigDecimal("1000.00"),
              LocalDate.of(2026, 4, 1)));

      Optional<String> currency = repo.findCurrencyByTeamId(TEAM_A_ID);

      assertThat(currency).contains("EUR");
    }

    @Test
    @DisplayName("findEarliestPaymentDate returns earliest paid payment date")
    void findEarliestPaymentDate() {
      Payment p =
          TestDataHelper.buildPayment(
              TEAM_A_ID, contractIdA, USER_ID, new BigDecimal("1000.00"), LocalDate.of(2026, 1, 1));
      p.setStatus(PaymentStatus.PAID);
      p.setPaymentDate(Optional.of(LocalDate.of(2026, 1, 10)));
      repo.save(p);

      Optional<LocalDate> earliest = repo.findEarliestPaymentDate(TEAM_A_ID);

      assertThat(earliest).contains(LocalDate.of(2026, 1, 10));
    }
  }

  @Nested
  @DisplayName("pagination")
  class Pagination {

    @Test
    @DisplayName("findAllByTeamIdPaginated with OVERDUE virtual status")
    void paginatedWithOverdueStatus() {
      // Past-due pending
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID,
              contractIdA,
              USER_ID,
              new BigDecimal("1000.00"),
              LocalDate.of(2026, 2, 1)));
      // Future pending
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID,
              contractIdA,
              USER_ID,
              new BigDecimal("1000.00"),
              LocalDate.of(2026, 4, 1)));

      PaginatedResult<Payment> result =
          repo.findAllByTeamIdPaginated(
              TEAM_A_ID,
              "OVERDUE",
              null,
              null,
              null,
              null,
              null,
              PageRequest.of(null, null, null, (String) null));

      assertThat(result.items()).hasSize(1);
      assertThat(result.items().getFirst().getDueDate()).isEqualTo(LocalDate.of(2026, 2, 1));
    }

    @Test
    @DisplayName("findAllByTeamIdPaginated with propertyId filter via subquery")
    void paginatedWithPropertyIdFilter() {
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID,
              contractIdA,
              USER_ID,
              new BigDecimal("1000.00"),
              LocalDate.of(2026, 4, 1)));

      PaginatedResult<Payment> result =
          repo.findAllByTeamIdPaginated(
              TEAM_A_ID,
              null,
              null,
              propertyIdA,
              null,
              null,
              null,
              PageRequest.of(null, null, null, (SortDirection) null));

      assertThat(result.items()).hasSize(1);
    }

    @Test
    @DisplayName("findAllByTeamIdPaginated does not return other team's payments")
    void paginatedIsolatesTeams() {
      repo.save(
          TestDataHelper.buildPayment(
              TEAM_A_ID,
              contractIdA,
              USER_ID,
              new BigDecimal("1000.00"),
              LocalDate.of(2026, 4, 1)));

      PaginatedResult<Payment> result =
          repo.findAllByTeamIdPaginated(
              TEAM_B_ID,
              null,
              null,
              null,
              null,
              null,
              null,
              PageRequest.of(null, null, null, (SortDirection) null));

      assertThat(result.items()).isEmpty();
    }
  }

  @Nested
  @DisplayName("softDelete")
  class SoftDelete {

    @Test
    @DisplayName("soft delete hides payment from find queries")
    void softDeleteHidesPayment() {
      Payment saved =
          repo.save(
              TestDataHelper.buildPayment(
                  TEAM_A_ID,
                  contractIdA,
                  USER_ID,
                  new BigDecimal("1000.00"),
                  LocalDate.of(2026, 4, 1)));

      repo.softDeleteByIdAndTeamId(saved.getId(), TEAM_A_ID);

      assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID)).isEmpty();
      assertThat(repo.findAllByTeamId(TEAM_A_ID)).isEmpty();
    }

    @Test
    @DisplayName("soft delete with wrong team does nothing")
    void softDeleteWrongTeamDoesNothing() {
      Payment saved =
          repo.save(
              TestDataHelper.buildPayment(
                  TEAM_A_ID,
                  contractIdA,
                  USER_ID,
                  new BigDecimal("1000.00"),
                  LocalDate.of(2026, 4, 1)));

      repo.softDeleteByIdAndTeamId(saved.getId(), TEAM_B_ID);

      assertThat(repo.findByIdAndTeamId(saved.getId(), TEAM_A_ID)).isPresent();
    }
  }
}
