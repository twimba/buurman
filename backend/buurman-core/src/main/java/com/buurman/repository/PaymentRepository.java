package com.buurman.repository;

import static com.buurman.domain.Payment.PaymentStatus.OVERDUE;
import static com.buurman.domain.Payment.PaymentStatus.PAID;
import static com.buurman.domain.Payment.PaymentStatus.PARTIALLY_PAID;
import static com.buurman.domain.Payment.PaymentStatus.PENDING;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static java.time.ZoneOffset.UTC;
import static java.util.Objects.requireNonNull;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.countDistinct;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.min;
import static org.jooq.impl.DSL.sum;
import static org.jooq.impl.DSL.table;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.AmountStats;
import com.buurman.domain.MonthlyAmount;
import com.buurman.domain.Payment;
import com.buurman.domain.Sid;
import com.buurman.dto.request.PageRequest;
import com.buurman.dto.response.BalanceStatus;
import com.buurman.dto.response.ContactBalanceSummary;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.PaymentRecordMapper;
import com.buurman.util.MoneyAmount;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
@RequiredArgsConstructor
public class PaymentRepository {

  private final DSLContext dsl;
  private final PaymentRecordMapper mapper;
  private final Clock clock;

  public Optional<Payment> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .IDENTIFIER
                .eq(identifier)
                .and(PAYMENTS.TEAM_ID.eq(teamId))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public Payment getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Payment not found"));
  }

  public Optional<Payment> findByIdAndTeamId(UUID id, UUID teamId) {
    return dsl.selectFrom(PAYMENTS)
        .where(
            PAYMENTS.ID.eq(id).and(PAYMENTS.TEAM_ID.eq(teamId)).and(PAYMENTS.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public List<Payment> findAllByTeamId(UUID teamId) {
    return dsl
        .selectFrom(PAYMENTS)
        .where(PAYMENTS.TEAM_ID.eq(teamId).and(PAYMENTS.DELETED_AT.isNull()))
        .orderBy(PAYMENTS.DUE_DATE.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public List<Payment> findByContactIdAndTeamId(UUID contactId, UUID teamId) {
    return dsl
        .selectFrom(PAYMENTS)
        .where(
            contactLinkedCondition(contactId, teamId)
                .and(PAYMENTS.TEAM_ID.eq(teamId))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .orderBy(PAYMENTS.DUE_DATE.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public List<Payment> findByContractId(UUID contractId, UUID teamId) {
    return dsl
        .selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .CONTRACT_ID
                .eq(contractId)
                .and(PAYMENTS.TEAM_ID.eq(teamId))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .orderBy(PAYMENTS.DUE_DATE.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  /**
   * Cash-basis lookup: paid rent whose {@code payment_date} fell in the window. Use for cash-flow
   * charts that bucket actual cash movement by month.
   */
  public List<Payment> findPaidByContractIdsAndDateRange(
      Collection<UUID> contractIds, UUID teamId, LocalDate from, LocalDate to) {
    if (contractIds.isEmpty()) {
      return List.of();
    }
    return dsl
        .selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .CONTRACT_ID
                .in(contractIds)
                .and(PAYMENTS.TEAM_ID.eq(teamId))
                .and(PAYMENTS.STATUS.eq(PAID.name()))
                .and(PAYMENTS.PAYMENT_DATE.isNotNull())
                .and(PAYMENTS.PAYMENT_DATE.between(from, to))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .orderBy(PAYMENTS.PAYMENT_DATE.asc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  /**
   * Accrual-basis lookup: paid rent whose {@code due_date} (rental period) fell in the window. Use
   * for averages that divide by calendar months — prevents prepaid future rent from inflating the
   * numerator while the calendar denominator stays flat.
   */
  public List<Payment> findPaidByContractIdsAndDueDateRange(
      Collection<UUID> contractIds, UUID teamId, LocalDate from, LocalDate to) {
    if (contractIds.isEmpty()) {
      return List.of();
    }
    return dsl
        .selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .CONTRACT_ID
                .in(contractIds)
                .and(PAYMENTS.TEAM_ID.eq(teamId))
                .and(PAYMENTS.STATUS.eq(PAID.name()))
                .and(PAYMENTS.DUE_DATE.between(from, to))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .orderBy(PAYMENTS.DUE_DATE.asc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  /**
   * A payment is overdue when it still has an open balance (PENDING, PARTIALLY_PAID or a stored
   * OVERDUE status) and its due date is strictly in the past. Shared by every overdue query so the
   * list filter, the stats card, the arrears view and the reminder job agree.
   */
  private Condition overdueCondition(LocalDate today) {
    return PAYMENTS
        .STATUS
        .in(PENDING.name(), PARTIALLY_PAID.name(), OVERDUE.name())
        .and(PAYMENTS.DUE_DATE.lt(today));
  }

  public List<Payment> findOverduePayments(UUID teamId) {
    LocalDate today = LocalDate.now(clock);
    return dsl
        .selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .TEAM_ID
                .eq(teamId)
                .and(overdueCondition(today))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .orderBy(PAYMENTS.DUE_DATE.asc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public List<Payment> findByDateRange(LocalDate startDate, LocalDate endDate, UUID teamId) {
    return dsl
        .selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .TEAM_ID
                .eq(teamId)
                .and(PAYMENTS.PAYMENT_DATE.isNotNull())
                .and(PAYMENTS.PAYMENT_DATE.between(startDate, endDate))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .orderBy(PAYMENTS.PAYMENT_DATE.asc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public Payment save(Payment payment) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (payment.getId() == null) {
      // Insert
      UUID id = UUID.randomUUID();
      LocalDateTime createdAt =
          payment.getCreatedAt() != null
              ? LocalDateTime.ofInstant(payment.getCreatedAt(), UTC)
              : now;
      LocalDateTime updatedAt =
          payment.getUpdatedAt() != null
              ? LocalDateTime.ofInstant(payment.getUpdatedAt(), UTC)
              : now;

      dsl.insertInto(PAYMENTS)
          .set(PAYMENTS.ID, id)
          .set(PAYMENTS.IDENTIFIER, payment.getIdentifier().orElseThrow())
          .set(PAYMENTS.TEAM_ID, payment.getTeamId())
          .set(PAYMENTS.CONTRACT_ID, payment.getContractId())
          .set(PAYMENTS.AMOUNT, payment.getAmount().value())
          .set(PAYMENTS.CURRENCY, payment.getAmount().currency())
          .set(PAYMENTS.PAYMENT_DATE, payment.getPaymentDate().orElse(null))
          .set(PAYMENTS.DUE_DATE, payment.getDueDate())
          .set(PAYMENTS.STATUS, payment.getStatus().name())
          .set(PAYMENTS.NOTES, payment.getNotes().orElse(null))
          .set(PAYMENTS.CONTACT_ID, payment.getContactId().orElse(null))
          .set(
              PAYMENTS.AUTO_GENERATED,
              payment.getAutoGenerated() != null ? payment.getAutoGenerated() : false)
          .set(PAYMENTS.PAYMENT_TYPE, payment.getPaymentType().name())
          .set(PAYMENTS.PARENT_PAYMENT_ID, payment.getParentPaymentId().orElse(null))
          .set(PAYMENTS.CANCEL_REASON, payment.getCancelReason().orElse(null))
          .set(PAYMENTS.CREATED_AT, createdAt)
          .set(PAYMENTS.UPDATED_AT, updatedAt)
          .set(PAYMENTS.CREATED_BY, payment.getCreatedBy())
          .set(PAYMENTS.UPDATED_BY, payment.getUpdatedBy())
          .execute();

      payment.setId(id);
      payment.setCreatedAt(createdAt.toInstant(UTC));
      payment.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      // Update
      LocalDateTime updatedAt =
          payment.getUpdatedAt() != null
              ? LocalDateTime.ofInstant(payment.getUpdatedAt(), UTC)
              : now;

      dsl.update(PAYMENTS)
          .set(PAYMENTS.AMOUNT, payment.getAmount().value())
          .set(PAYMENTS.CURRENCY, payment.getAmount().currency())
          .set(PAYMENTS.PAYMENT_DATE, payment.getPaymentDate().orElse(null))
          .set(PAYMENTS.DUE_DATE, payment.getDueDate())
          .set(PAYMENTS.STATUS, payment.getStatus().name())
          .set(PAYMENTS.NOTES, payment.getNotes().orElse(null))
          .set(PAYMENTS.CONTACT_ID, payment.getContactId().orElse(null))
          .set(PAYMENTS.CANCEL_REASON, payment.getCancelReason().orElse(null))
          .set(
              PAYMENTS.WAIVED_AT,
              payment.getWaivedAt().map(i -> LocalDateTime.ofInstant(i, UTC)).orElse(null))
          .set(PAYMENTS.WAIVED_BY, payment.getWaivedBy().orElse(null))
          .set(PAYMENTS.WAIVE_REASON, payment.getWaiveReason().orElse(null))
          .set(PAYMENTS.UPDATED_AT, updatedAt)
          .set(PAYMENTS.UPDATED_BY, payment.getUpdatedBy())
          .where(PAYMENTS.ID.eq(payment.getId()).and(PAYMENTS.TEAM_ID.eq(payment.getTeamId())))
          .execute();

      payment.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return payment;
  }

  public boolean existsByContractIdAndDueDate(UUID contractId, LocalDate dueDate) {
    return dsl.fetchExists(
        dsl.selectFrom(PAYMENTS)
            .where(
                PAYMENTS
                    .CONTRACT_ID
                    .eq(contractId)
                    .and(PAYMENTS.DUE_DATE.eq(dueDate))
                    .and(PAYMENTS.DELETED_AT.isNull())));
  }

  public List<Payment> findPendingByContractIdFromDate(
      UUID contractId, UUID teamId, LocalDate fromDate) {
    return dsl
        .selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .CONTRACT_ID
                .eq(contractId)
                .and(PAYMENTS.TEAM_ID.eq(teamId))
                .and(PAYMENTS.STATUS.eq(PENDING.name()))
                .and(PAYMENTS.DUE_DATE.ge(fromDate))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public List<Payment> findFuturePendingByContractId(UUID contractId, UUID teamId) {
    LocalDate today = LocalDate.now(clock);
    return dsl
        .selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .CONTRACT_ID
                .eq(contractId)
                .and(PAYMENTS.TEAM_ID.eq(teamId))
                .and(PAYMENTS.STATUS.eq(PENDING.name()))
                .and(PAYMENTS.DUE_DATE.gt(today))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public PaginatedResult<Payment> findAllByTeamIdPaginated(
      UUID teamId,
      @Nullable String status,
      @Nullable UUID contractId,
      @Nullable UUID propertyId,
      @Nullable UUID contactId,
      @Nullable LocalDate dateFrom,
      @Nullable LocalDate dateTo,
      PageRequest pageRequest) {
    Condition condition = PAYMENTS.TEAM_ID.eq(teamId).and(PAYMENTS.DELETED_AT.isNull());
    if (status != null && !status.isEmpty()) {
      if ("OVERDUE".equalsIgnoreCase(status)) {
        condition = condition.and(overdueCondition(LocalDate.now(clock)));
      } else {
        condition = condition.and(PAYMENTS.STATUS.eq(status));
      }
    }
    if (contractId != null) {
      condition = condition.and(PAYMENTS.CONTRACT_ID.eq(contractId));
    }
    if (contactId != null) {
      condition = condition.and(contactLinkedCondition(contactId, teamId));
    }
    if (propertyId != null) {
      condition =
          condition.and(
              PAYMENTS.CONTRACT_ID.in(
                  dsl.select(CONTRACTS.ID)
                      .from(CONTRACTS)
                      .where(
                          CONTRACTS
                              .PROPERTY_ID
                              .eq(propertyId)
                              .and(CONTRACTS.DELETED_AT.isNull()))));
    }
    if (dateFrom != null) {
      condition = condition.and(PAYMENTS.DUE_DATE.ge(dateFrom));
    }
    if (dateTo != null) {
      condition = condition.and(PAYMENTS.DUE_DATE.le(dateTo));
    }
    Map<String, Field<?>> sortableFields =
        Map.of(
            "dueDate", PAYMENTS.DUE_DATE,
            "amount", PAYMENTS.AMOUNT,
            "status", PAYMENTS.STATUS,
            "createdAt", PAYMENTS.CREATED_AT);
    return PaginationHelper.paginate(
        dsl,
        PAYMENTS,
        condition,
        sortableFields,
        PAYMENTS.DUE_DATE,
        pageRequest,
        r ->
            mapper
                .toDomain(r)
                .orElseThrow(() -> new IllegalStateException("Failed to map payment record")));
  }

  public Optional<AmountStats> getPendingStats(UUID teamId) {
    return dsl.select(count().as("count"), sum(PAYMENTS.AMOUNT).as("total"))
        .from(PAYMENTS)
        .where(
            PAYMENTS
                .TEAM_ID
                .eq(teamId)
                .and(PAYMENTS.STATUS.in(PENDING.name(), PARTIALLY_PAID.name()))
                .and(PAYMENTS.DUE_DATE.ge(LocalDate.now(clock)))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(
            r ->
                new AmountStats(
                    r.value1() != null ? r.value1() : 0, Optional.ofNullable(r.value2())));
  }

  /**
   * Overdue stats count every past-due payment with an open balance; the total is what is still
   * outstanding (amount minus receivals), not the face value.
   */
  public Optional<AmountStats> getOverdueStats(UUID teamId) {
    var receivals = receivedPerPaymentSubquery(teamId);
    Field<UUID> rPaymentId = requireNonNull(receivals.field("r_payment_id", UUID.class));
    Field<BigDecimal> received = requireNonNull(receivals.field("received", BigDecimal.class));
    Field<Long> rawAmount = field("payments.amount", Long.class);
    return dsl.select(
            count().as("count"),
            sum(rawAmount.minus(DSL.coalesce(received, BigDecimal.ZERO))).as("total"))
        .from(PAYMENTS)
        .leftJoin(receivals)
        .on(rPaymentId.eq(PAYMENTS.ID))
        .where(
            PAYMENTS
                .TEAM_ID
                .eq(teamId)
                .and(overdueCondition(LocalDate.now(clock)))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(
            r ->
                new AmountStats(
                    r.value1() != null ? r.value1() : 0, Optional.ofNullable(r.value2())));
  }

  /**
   * One row per overdue payment with its open balance, for the arrears view. Kept as a flat
   * projection rather than domain objects so the service can age and group without N+1 lookups.
   */
  public List<OverduePaymentRow> findOverdueRows(UUID teamId) {
    LocalDate today = LocalDate.now(clock);
    var receivals = receivedPerPaymentSubquery(teamId);
    Field<UUID> rPaymentId = requireNonNull(receivals.field("r_payment_id", UUID.class));
    Field<BigDecimal> received = requireNonNull(receivals.field("received", BigDecimal.class));
    Field<Long> rawAmount = field("payments.amount", Long.class);
    return dsl
        .select(
            PAYMENTS.ID,
            PAYMENTS.IDENTIFIER,
            PAYMENTS.CONTRACT_ID,
            PAYMENTS.CONTACT_ID,
            PAYMENTS.CURRENCY,
            rawAmount.as("amount_minor"),
            DSL.coalesce(received, BigDecimal.ZERO).as("received"),
            PAYMENTS.DUE_DATE)
        .from(PAYMENTS)
        .leftJoin(receivals)
        .on(rPaymentId.eq(PAYMENTS.ID))
        .where(
            PAYMENTS
                .TEAM_ID
                .eq(teamId)
                .and(overdueCondition(today))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .orderBy(PAYMENTS.DUE_DATE.asc())
        .fetch()
        .stream()
        .map(
            r -> {
              String currency = r.get(PAYMENTS.CURRENCY);
              BigDecimal amount =
                  MoneyAmount.sumToMajorUnits(r.get("amount_minor", BigDecimal.class), currency);
              BigDecimal receivedMajor =
                  MoneyAmount.sumToMajorUnits(r.get("received", BigDecimal.class), currency);
              return new OverduePaymentRow(
                  r.get(PAYMENTS.ID),
                  r.get(PAYMENTS.IDENTIFIER),
                  r.get(PAYMENTS.CONTRACT_ID),
                  Optional.ofNullable(r.get(PAYMENTS.CONTACT_ID)),
                  currency,
                  amount.subtract(receivedMajor),
                  r.get(PAYMENTS.DUE_DATE));
            })
        .filter(row -> row.outstanding().compareTo(BigDecimal.ZERO) > 0)
        .toList();
  }

  /**
   * Open payments (PENDING / PARTIALLY_PAID / OVERDUE) due on or before the given date, oldest
   * first. The dunning scheduler uses this with {@code today - minOffset} so pre-due steps work.
   */
  public List<Payment> findOpenPaymentsDueOnOrBefore(UUID teamId, LocalDate dueDateInclusive) {
    return dsl
        .selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .TEAM_ID
                .eq(teamId)
                .and(PAYMENTS.STATUS.in(PENDING.name(), PARTIALLY_PAID.name(), OVERDUE.name()))
                .and(PAYMENTS.DUE_DATE.le(dueDateInclusive))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .orderBy(PAYMENTS.DUE_DATE.asc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  /**
   * Open LATE_FEE payments charged on the given rent payment (non-deleted, any status but
   * CANCELLED).
   */
  public boolean hasLateFee(UUID parentPaymentId, UUID teamId) {
    return dsl.fetchExists(
        dsl.selectFrom(PAYMENTS)
            .where(
                PAYMENTS
                    .PARENT_PAYMENT_ID
                    .eq(parentPaymentId)
                    .and(PAYMENTS.TEAM_ID.eq(teamId))
                    .and(PAYMENTS.PAYMENT_TYPE.eq(Payment.PaymentType.LATE_FEE.name()))
                    .and(PAYMENTS.DELETED_AT.isNull())));
  }

  /**
   * Overdue RENT payments on contracts with late fees enabled whose grace period has elapsed,
   * across all teams. The scheduler charges each of these once (see {@link #hasLateFee}).
   */
  public List<Payment> findLateFeeCandidates(LocalDate today) {
    return dsl
        .select(PAYMENTS.fields())
        .from(PAYMENTS)
        .join(CONTRACTS)
        .on(CONTRACTS.ID.eq(PAYMENTS.CONTRACT_ID))
        .where(
            PAYMENTS
                .PAYMENT_TYPE
                .eq(Payment.PaymentType.RENT.name())
                .and(PAYMENTS.STATUS.in(PENDING.name(), PARTIALLY_PAID.name(), OVERDUE.name()))
                .and(PAYMENTS.DELETED_AT.isNull())
                .and(CONTRACTS.DELETED_AT.isNull())
                .and(CONTRACTS.LATE_FEE_ENABLED.isTrue())
                .and(CONTRACTS.LATE_FEE_PERCENTAGE.gt(java.math.BigDecimal.ZERO))
                .and(
                    PAYMENTS.DUE_DATE.lt(
                        DSL.localDateSub(
                            DSL.inline(today),
                            DSL.coalesce(CONTRACTS.LATE_FEE_GRACE_DAYS, DSL.inline(0))))))
        .orderBy(PAYMENTS.DUE_DATE.asc())
        .fetch()
        .stream()
        .map(r -> mapper.toDomain(r.into(PAYMENTS)))
        .flatMap(Optional::stream)
        .toList();
  }

  public record OverduePaymentRow(
      UUID paymentId,
      Sid identifier,
      UUID contractId,
      Optional<UUID> contactId,
      String currency,
      BigDecimal outstanding,
      LocalDate dueDate) {}

  public List<Payment> findByIdentifiersAndTeamId(Collection<Sid> identifiers, UUID teamId) {
    if (identifiers.isEmpty()) {
      return List.of();
    }
    return dsl
        .selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .TEAM_ID
                .eq(teamId)
                .and(PAYMENTS.IDENTIFIER.in(identifiers))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  /**
   * sum(receivals) per payment for the team, aliased as (r_payment_id, received) in minor units.
   */
  private Table<?> receivedPerPaymentSubquery(UUID teamId) {
    Table<?> receivalsTable = table("payment_receivals");
    Field<UUID> rPaymentId = field("payment_id", UUID.class);
    Field<UUID> rTeamId = field("team_id", UUID.class);
    Field<Long> rAmount = field("amount", Long.class);
    Field<LocalDateTime> rDeletedAt = field("deleted_at", LocalDateTime.class);
    return dsl.select(rPaymentId.as("r_payment_id"), sum(rAmount).as("received"))
        .from(receivalsTable)
        .where(rDeletedAt.isNull().and(rTeamId.eq(teamId)))
        .groupBy(rPaymentId)
        .asTable("r");
  }

  public List<MonthlyAmount> getMonthlyPaidTrend(UUID teamId, int months) {
    LocalDate startDate = LocalDate.now(clock).minusMonths(months).withDayOfMonth(1);
    return dsl.select(
            field("to_char({0}, 'YYYY-MM')", String.class, PAYMENTS.PAYMENT_DATE).as("month"),
            sum(PAYMENTS.AMOUNT).as("total"))
        .from(PAYMENTS)
        .where(
            PAYMENTS
                .TEAM_ID
                .eq(teamId)
                .and(PAYMENTS.STATUS.eq(PAID.name()))
                .and(PAYMENTS.PAYMENT_DATE.isNotNull())
                .and(PAYMENTS.PAYMENT_DATE.ge(startDate))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .groupBy(field("to_char({0}, 'YYYY-MM')", String.class, PAYMENTS.PAYMENT_DATE))
        .orderBy(field("to_char({0}, 'YYYY-MM')", String.class, PAYMENTS.PAYMENT_DATE).asc())
        .fetch()
        .map(r -> new MonthlyAmount(r.value1(), Optional.ofNullable(r.value2())));
  }

  public Optional<String> findCurrencyByTeamId(UUID teamId) {
    return dsl.select(PAYMENTS.CURRENCY)
        .from(PAYMENTS)
        .where(PAYMENTS.TEAM_ID.eq(teamId).and(PAYMENTS.DELETED_AT.isNull()))
        .limit(1)
        .fetchOptional()
        .map(r -> r.get(PAYMENTS.CURRENCY));
  }

  public Optional<LocalDate> findEarliestPaymentDate(UUID teamId) {
    return Optional.ofNullable(
        dsl.select(min(PAYMENTS.PAYMENT_DATE))
            .from(PAYMENTS)
            .where(
                PAYMENTS
                    .TEAM_ID
                    .eq(teamId)
                    .and(PAYMENTS.DELETED_AT.isNull())
                    .and(PAYMENTS.PAYMENT_DATE.isNotNull())
                    .and(PAYMENTS.STATUS.eq(PAID.name())))
            .fetchOne(min(PAYMENTS.PAYMENT_DATE)));
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(PAYMENTS)
        .set(PAYMENTS.DELETED_AT, now)
        .where(PAYMENTS.ID.eq(id).and(PAYMENTS.TEAM_ID.eq(teamId)))
        .execute();
  }

  /**
   * Batch-loads outstanding balance per contact for a collection of contact IDs. Outstanding =
   * sum(payment amounts) - sum(receivals) for PENDING/PARTIALLY_PAID/OVERDUE payments whose due
   * date is today or in the past. Uses payments.contact_id (PRIMARY_TENANT link only, no
   * double-counting via contract_parties).
   */
  public Map<UUID, ContactBalanceSummary> getOutstandingBalancesByContactIds(
      Collection<UUID> contactIds, UUID teamId) {
    if (contactIds.isEmpty()) {
      return Map.of();
    }

    LocalDate today = LocalDate.now(clock);

    // Raw table/field refs — payment_receivals and contract_parties have no JOOQ generated class
    Table<?> receivalsTable = table("payment_receivals");
    Field<UUID> rPaymentId = field("payment_id", UUID.class);
    Field<UUID> rTeamId = field("team_id", UUID.class);
    Field<Long> rAmount = field("amount", Long.class);
    Field<LocalDateTime> rDeletedAt = field("deleted_at", LocalDateTime.class);

    Table<?> cpTable = table("contract_parties");
    Field<UUID> cpContactId = field("contract_parties.contact_id", UUID.class);
    Field<UUID> cpContractId = field("contract_parties.contract_id", UUID.class);
    Field<UUID> cpTeamId = field("contract_parties.team_id", UUID.class);
    Field<String> cpRole = field("contract_parties.role", String.class);
    Field<LocalDateTime> cpDeletedAt = field("contract_parties.deleted_at", LocalDateTime.class);

    Field<UUID> rPaymentIdAlias = rPaymentId.as("r_payment_id");
    Field<BigDecimal> receivedAlias = sum(rAmount).as("received");

    var receivalsSubquery =
        dsl.select(rPaymentIdAlias, receivedAlias)
            .from(receivalsTable)
            .where(rDeletedAt.isNull().and(rTeamId.eq(teamId)))
            .groupBy(rPaymentId)
            .asTable("r");

    Field<UUID> joinField = requireNonNull(receivalsSubquery.field(rPaymentIdAlias));
    Field<BigDecimal> receivedField = requireNonNull(receivalsSubquery.field(receivedAlias));

    var result =
        dsl.select(
                cpContactId.as("cp_contact_id"),
                PAYMENTS.CURRENCY,
                sum(PAYMENTS.AMOUNT).as("total_owed"),
                DSL.coalesce(sum(receivedField), BigDecimal.ZERO).as("total_received"),
                DSL.max(
                        DSL.when(PAYMENTS.DUE_DATE.lt(today), DSL.inline(1))
                            .otherwise(DSL.inline(0)))
                    .as("has_overdue"),
                countDistinct(PAYMENTS.ID).as("payment_count"))
            .from(PAYMENTS)
            .join(cpTable)
            .on(
                cpContractId
                    .eq(PAYMENTS.CONTRACT_ID)
                    .and(cpTeamId.eq(teamId))
                    .and(cpRole.eq("PRIMARY_TENANT"))
                    .and(cpDeletedAt.isNull()))
            .leftJoin(receivalsSubquery)
            .on(joinField.eq(PAYMENTS.ID))
            .where(
                cpContactId
                    .in(contactIds)
                    .and(PAYMENTS.TEAM_ID.eq(teamId))
                    .and(PAYMENTS.STATUS.in(PENDING.name(), PARTIALLY_PAID.name(), OVERDUE.name()))
                    .and(PAYMENTS.DUE_DATE.le(today))
                    .and(PAYMENTS.DELETED_AT.isNull()))
            .groupBy(cpContactId, PAYMENTS.CURRENCY)
            .fetch();

    Map<UUID, ContactBalanceSummary> balances = new HashMap<>();
    for (var row : result) {
      UUID contactId = row.get("cp_contact_id", UUID.class);
      if (balances.containsKey(contactId)) {
        log.warn(
            "Contact {} has outstanding payments in multiple currencies; showing first only",
            contactId);
        continue;
      }
      String currency = row.get(PAYMENTS.CURRENCY);
      BigDecimal totalOwed = row.get("total_owed", BigDecimal.class);
      BigDecimal totalReceived = row.get("total_received", BigDecimal.class);
      Integer hasOverdueFlag = row.get("has_overdue", Integer.class);
      Integer paymentCount = row.get("payment_count", Integer.class);

      BigDecimal outstanding =
          MoneyAmount.sumToMajorUnits(totalOwed, currency)
              .subtract(MoneyAmount.sumToMajorUnits(totalReceived, currency));

      if (outstanding.compareTo(BigDecimal.ZERO) > 0) {
        boolean overdue = hasOverdueFlag != null && hasOverdueFlag > 0;
        balances.put(
            contactId,
            new ContactBalanceSummary(
                outstanding,
                currency,
                overdue ? BalanceStatus.OVERDUE : BalanceStatus.PENDING,
                paymentCount != null ? paymentCount : 0,
                Optional.empty(),
                Optional.empty()));
      }
    }
    return balances;
  }

  /**
   * Returns the subset of contactIds that have at least one non-deleted, non-cancelled payment (any
   * status). Used to distinguish "all paid" contacts from "no financial data" contacts.
   */
  public Set<UUID> getContactIdsWithPaymentHistory(Collection<UUID> contactIds, UUID teamId) {
    if (contactIds.isEmpty()) {
      return Set.of();
    }

    Table<?> cpTable = table("contract_parties");
    Field<UUID> cpContactId = field("contract_parties.contact_id", UUID.class);
    Field<UUID> cpContractId = field("contract_parties.contract_id", UUID.class);
    Field<UUID> cpTeamId = field("contract_parties.team_id", UUID.class);
    Field<String> cpRole = field("contract_parties.role", String.class);
    Field<LocalDateTime> cpDeletedAt = field("contract_parties.deleted_at", LocalDateTime.class);

    return new java.util.HashSet<>(
        dsl.select(cpContactId)
            .from(PAYMENTS)
            .join(cpTable)
            .on(
                cpContractId
                    .eq(PAYMENTS.CONTRACT_ID)
                    .and(cpTeamId.eq(teamId))
                    .and(cpRole.eq("PRIMARY_TENANT"))
                    .and(cpDeletedAt.isNull()))
            .where(
                cpContactId
                    .in(contactIds)
                    .and(PAYMENTS.TEAM_ID.eq(teamId))
                    .and(PAYMENTS.STATUS.ne("CANCELLED"))
                    .and(PAYMENTS.DELETED_AT.isNull()))
            .groupBy(cpContactId)
            .fetch(cpContactId));
  }

  /**
   * Batch-loads guaranteed balance per contact for contacts who are GUARANTOR on contracts. Joins
   * contract_parties (GUARANTOR role) → contracts → payments, subtracting receivals to get net
   * outstanding guaranteed amount.
   */
  public Map<UUID, GuaranteedBalance> getGuaranteedBalancesByContactIds(
      Collection<UUID> contactIds, UUID teamId) {
    if (contactIds.isEmpty()) {
      return Map.of();
    }

    LocalDate today = LocalDate.now(clock);

    Table<?> cpTable = table("contract_parties");
    Field<UUID> cpContactId = field("contract_parties.contact_id", UUID.class);
    Field<UUID> cpContractId = field("contract_parties.contract_id", UUID.class);
    Field<UUID> cpTeamId = field("contract_parties.team_id", UUID.class);
    Field<String> cpRole = field("contract_parties.role", String.class);
    Field<LocalDateTime> cpDeletedAt = field("contract_parties.deleted_at", LocalDateTime.class);

    // Receivals subquery (same as outstanding query)
    Table<?> receivalsTable = table("payment_receivals");
    Field<UUID> rPaymentId = field("payment_id", UUID.class);
    Field<UUID> rTeamId = field("team_id", UUID.class);
    Field<Long> rAmount = field("amount", Long.class);
    Field<LocalDateTime> rDeletedAt = field("deleted_at", LocalDateTime.class);

    Field<UUID> rPaymentIdAlias = rPaymentId.as("gr_payment_id");
    Field<BigDecimal> receivedAlias = sum(rAmount).as("gr_received");

    var receivalsSubquery =
        dsl.select(rPaymentIdAlias, receivedAlias)
            .from(receivalsTable)
            .where(rDeletedAt.isNull().and(rTeamId.eq(teamId)))
            .groupBy(rPaymentId)
            .asTable("gr");

    Field<UUID> joinField = requireNonNull(receivalsSubquery.field(rPaymentIdAlias));
    Field<BigDecimal> receivedField = requireNonNull(receivalsSubquery.field(receivedAlias));

    var result =
        dsl.select(
                cpContactId.as("guarantor_contact_id"),
                PAYMENTS.CURRENCY,
                sum(PAYMENTS.AMOUNT).as("total_owed"),
                DSL.coalesce(sum(receivedField), BigDecimal.ZERO).as("total_received"),
                countDistinct(PAYMENTS.ID).as("payment_count"))
            .from(cpTable)
            .join(PAYMENTS)
            .on(
                PAYMENTS
                    .CONTRACT_ID
                    .eq(cpContractId)
                    .and(PAYMENTS.TEAM_ID.eq(teamId))
                    .and(PAYMENTS.STATUS.in(PENDING.name(), PARTIALLY_PAID.name(), OVERDUE.name()))
                    .and(PAYMENTS.DUE_DATE.le(today))
                    .and(PAYMENTS.DELETED_AT.isNull()))
            .leftJoin(receivalsSubquery)
            .on(joinField.eq(PAYMENTS.ID))
            .where(
                cpContactId
                    .in(contactIds)
                    .and(cpTeamId.eq(teamId))
                    .and(cpRole.eq("GUARANTOR"))
                    .and(cpDeletedAt.isNull()))
            .groupBy(cpContactId, PAYMENTS.CURRENCY)
            .fetch();

    Map<UUID, GuaranteedBalance> guarantees = new HashMap<>();
    for (var row : result) {
      UUID contactId = row.get("guarantor_contact_id", UUID.class);
      if (guarantees.containsKey(contactId)) {
        log.warn(
            "Guarantor {} has guaranteed payments in multiple currencies; showing first only",
            contactId);
        continue;
      }
      String currency = row.get(PAYMENTS.CURRENCY);
      BigDecimal totalOwed = row.get("total_owed", BigDecimal.class);
      BigDecimal totalReceived = row.get("total_received", BigDecimal.class);
      Integer paymentCount = row.get("payment_count", Integer.class);

      BigDecimal guaranteed =
          MoneyAmount.sumToMajorUnits(totalOwed, currency)
              .subtract(MoneyAmount.sumToMajorUnits(totalReceived, currency));

      if (guaranteed.compareTo(BigDecimal.ZERO) > 0) {
        guarantees.put(
            contactId,
            new GuaranteedBalance(guaranteed, currency, paymentCount != null ? paymentCount : 0));
      }
    }
    return guarantees;
  }

  /**
   * Lightweight container for guaranteed balance data before merging into ContactBalanceSummary.
   */
  public record GuaranteedBalance(BigDecimal amount, String currency, int paymentCount) {}

  /**
   * Matches payments explicitly linked to a contact OR payments from contracts where the contact is
   * a party (e.g. primary tenant). This ensures the contact financials tab shows all relevant
   * payments, not just those with an explicit contact_id.
   */
  private Condition contactLinkedCondition(UUID contactId, UUID teamId) {
    return PAYMENTS
        .CONTACT_ID
        .eq(contactId)
        .or(
            PAYMENTS.CONTRACT_ID.in(
                dsl.select(field("contract_id", UUID.class))
                    .from(table("contract_parties"))
                    .where(
                        field("contact_id", UUID.class)
                            .eq(contactId)
                            .and(field("team_id", UUID.class).eq(teamId))
                            .and(field("deleted_at").isNull()))));
  }

  public int countByTeamId(UUID teamId) {
    return dsl.fetchCount(
        dsl.selectFrom(PAYMENTS)
            .where(PAYMENTS.TEAM_ID.eq(teamId).and(PAYMENTS.DELETED_AT.isNull())));
  }
}
