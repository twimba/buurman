package com.buurman.repository;

import static com.buurman.domain.Payment.PaymentStatus.PAID;
import static com.buurman.domain.Payment.PaymentStatus.PARTIALLY_PAID;
import static com.buurman.domain.Payment.PaymentStatus.PENDING;
import static com.buurman.jooq.generated.Tables.CONTRACTS;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.min;
import static org.jooq.impl.DSL.sum;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.AmountStats;
import com.buurman.domain.MonthlyAmount;
import com.buurman.domain.Payment;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.PaymentRecordMapper;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentRepository {

  private final DSLContext dsl;
  private final PaymentRecordMapper mapper;
  private final Clock clock;

  public Optional<Payment> findByIdentifierAndTeamId(String identifier, UUID teamId) {
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

  public Payment getByIdentifierAndTeamId(String identifier, UUID teamId) {
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

  public List<Payment> findOverduePayments(UUID teamId) {
    LocalDate today = LocalDate.now(clock);
    return dsl
        .selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .TEAM_ID
                .eq(teamId)
                .and(PAYMENTS.STATUS.eq(PENDING.name()))
                .and(PAYMENTS.DUE_DATE.lt(today))
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

      String currency = payment.getCurrency();
      dsl.insertInto(PAYMENTS)
          .set(PAYMENTS.ID, id)
          .set(PAYMENTS.IDENTIFIER, payment.getIdentifier())
          .set(PAYMENTS.TEAM_ID, payment.getTeamId())
          .set(PAYMENTS.CONTRACT_ID, payment.getContractId())
          .set(PAYMENTS.AMOUNT, CurrencyUtils.toMinorUnits(payment.getAmount(), currency))
          .set(PAYMENTS.CURRENCY, currency)
          .set(PAYMENTS.PAYMENT_DATE, payment.getPaymentDate().orElse(null))
          .set(PAYMENTS.DUE_DATE, payment.getDueDate())
          .set(PAYMENTS.STATUS, payment.getStatus().name())
          .set(PAYMENTS.NOTES, payment.getNotes().orElse(null))
          .set(
              PAYMENTS.AUTO_GENERATED,
              payment.getAutoGenerated() != null ? payment.getAutoGenerated() : false)
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

      String currency = payment.getCurrency();
      dsl.update(PAYMENTS)
          .set(PAYMENTS.AMOUNT, CurrencyUtils.toMinorUnits(payment.getAmount(), currency))
          .set(PAYMENTS.CURRENCY, currency)
          .set(PAYMENTS.PAYMENT_DATE, payment.getPaymentDate().orElse(null))
          .set(PAYMENTS.DUE_DATE, payment.getDueDate())
          .set(PAYMENTS.STATUS, payment.getStatus().name())
          .set(PAYMENTS.NOTES, payment.getNotes().orElse(null))
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
      @Nullable LocalDate dateFrom,
      @Nullable LocalDate dateTo,
      PageRequest pageRequest) {
    Condition condition = PAYMENTS.TEAM_ID.eq(teamId).and(PAYMENTS.DELETED_AT.isNull());
    if (status != null && !status.isEmpty()) {
      if ("OVERDUE".equalsIgnoreCase(status)) {
        condition =
            condition
                .and(PAYMENTS.STATUS.eq(PENDING.name()))
                .and(PAYMENTS.DUE_DATE.lt(LocalDate.now(clock)));
      } else {
        condition = condition.and(PAYMENTS.STATUS.eq(status));
      }
    }
    if (contractId != null) {
      condition = condition.and(PAYMENTS.CONTRACT_ID.eq(contractId));
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
        r -> mapper.toDomain(r).orElseThrow());
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

  public Optional<AmountStats> getOverdueStats(UUID teamId) {
    return dsl.select(count().as("count"), sum(PAYMENTS.AMOUNT).as("total"))
        .from(PAYMENTS)
        .where(
            PAYMENTS
                .TEAM_ID
                .eq(teamId)
                .and(PAYMENTS.STATUS.eq(PENDING.name()))
                .and(PAYMENTS.DUE_DATE.lt(LocalDate.now(clock)))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(
            r ->
                new AmountStats(
                    r.value1() != null ? r.value1() : 0, Optional.ofNullable(r.value2())));
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
}
