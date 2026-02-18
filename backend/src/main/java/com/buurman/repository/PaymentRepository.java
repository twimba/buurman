package com.buurman.repository;

import static com.buurman.domain.Payment.PaymentStatus.PAID;
import static com.buurman.domain.Payment.PaymentStatus.PARTIALLY_PAID;
import static com.buurman.domain.Payment.PaymentStatus.PENDING;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.count;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.sum;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record2;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Payment;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.jooq.generated.tables.records.PaymentsRecord;
import com.buurman.mapper.PaymentRecordMapper;
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
        .map(mapper::toDomain);
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
        .map(mapper::toDomain);
  }

  public Payment getByIdAndTeamId(UUID id, UUID teamId) {
    return findByIdAndTeamId(id, teamId)
        .orElseThrow(() -> new NotFoundException("Payment not found"));
  }

  public List<Payment> findAllByTeamId(UUID teamId) {
    return dsl.selectFrom(PAYMENTS)
        .where(PAYMENTS.TEAM_ID.eq(teamId).and(PAYMENTS.DELETED_AT.isNull()))
        .orderBy(PAYMENTS.DUE_DATE.desc())
        .fetch()
        .map(mapper::toDomain);
  }

  public List<Payment> findByContractId(UUID contractId, UUID teamId) {
    return dsl.selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .CONTRACT_ID
                .eq(contractId)
                .and(PAYMENTS.TEAM_ID.eq(teamId))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .orderBy(PAYMENTS.DUE_DATE.desc())
        .fetch()
        .map(mapper::toDomain);
  }

  public List<Payment> findByStatus(Payment.PaymentStatus status, UUID teamId) {
    return dsl.selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .STATUS
                .eq(status.name())
                .and(PAYMENTS.TEAM_ID.eq(teamId))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .orderBy(PAYMENTS.DUE_DATE.desc())
        .fetch()
        .map(mapper::toDomain);
  }

  public List<Payment> findOverduePayments(UUID teamId) {
    LocalDate today = LocalDate.now(clock);
    return dsl.selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .TEAM_ID
                .eq(teamId)
                .and(PAYMENTS.STATUS.eq(PENDING.name()))
                .and(PAYMENTS.DUE_DATE.lt(today))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .orderBy(PAYMENTS.DUE_DATE.asc())
        .fetch()
        .map(mapper::toDomain);
  }

  public List<Payment> findByDueDateRange(LocalDate startDate, LocalDate endDate, UUID teamId) {
    return dsl.selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .TEAM_ID
                .eq(teamId)
                .and(PAYMENTS.DUE_DATE.between(startDate, endDate))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .orderBy(PAYMENTS.DUE_DATE.asc())
        .fetch()
        .map(mapper::toDomain);
  }

  public List<Payment> findByDateRange(LocalDate startDate, LocalDate endDate, UUID teamId) {
    return dsl.selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .TEAM_ID
                .eq(teamId)
                .and(PAYMENTS.PAYMENT_DATE.isNotNull())
                .and(PAYMENTS.PAYMENT_DATE.between(startDate, endDate))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .orderBy(PAYMENTS.PAYMENT_DATE.asc())
        .fetch()
        .map(mapper::toDomain);
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
          .set(PAYMENTS.IDENTIFIER, payment.getIdentifier())
          .set(PAYMENTS.TEAM_ID, payment.getTeamId())
          .set(PAYMENTS.CONTRACT_ID, payment.getContractId())
          .set(PAYMENTS.AMOUNT, payment.getAmount())
          .set(PAYMENTS.CURRENCY, payment.getCurrency())
          .set(PAYMENTS.PAYMENT_DATE, payment.getPaymentDate())
          .set(PAYMENTS.DUE_DATE, payment.getDueDate())
          .set(PAYMENTS.STATUS, payment.getStatus().name())
          .set(PAYMENTS.NOTES, payment.getNotes())
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

      dsl.update(PAYMENTS)
          .set(PAYMENTS.AMOUNT, payment.getAmount())
          .set(PAYMENTS.CURRENCY, payment.getCurrency())
          .set(PAYMENTS.PAYMENT_DATE, payment.getPaymentDate())
          .set(PAYMENTS.DUE_DATE, payment.getDueDate())
          .set(PAYMENTS.STATUS, payment.getStatus().name())
          .set(PAYMENTS.NOTES, payment.getNotes())
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
    return dsl.selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .CONTRACT_ID
                .eq(contractId)
                .and(PAYMENTS.TEAM_ID.eq(teamId))
                .and(PAYMENTS.STATUS.eq(PENDING.name()))
                .and(PAYMENTS.DUE_DATE.ge(fromDate))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .fetch()
        .map(mapper::toDomain);
  }

  public List<Payment> findFuturePendingByContractId(UUID contractId, UUID teamId) {
    LocalDate today = LocalDate.now(clock);
    return dsl.selectFrom(PAYMENTS)
        .where(
            PAYMENTS
                .CONTRACT_ID
                .eq(contractId)
                .and(PAYMENTS.TEAM_ID.eq(teamId))
                .and(PAYMENTS.STATUS.eq(PENDING.name()))
                .and(PAYMENTS.DUE_DATE.gt(today))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .fetch()
        .map(mapper::toDomain);
  }

  public PaginatedResult<Payment> findAllByTeamIdPaginated(
      UUID teamId, String status, UUID contractId, PageRequest pageRequest) {
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
        r -> mapper.toDomain((PaymentsRecord) r));
  }

  public Record2<Integer, BigDecimal> getPendingStats(UUID teamId) {
    return dsl.select(count().as("count"), sum(PAYMENTS.AMOUNT).as("total"))
        .from(PAYMENTS)
        .where(
            PAYMENTS
                .TEAM_ID
                .eq(teamId)
                .and(PAYMENTS.STATUS.in(PENDING.name(), PARTIALLY_PAID.name()))
                .and(PAYMENTS.DUE_DATE.ge(LocalDate.now(clock)))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .fetchOne();
  }

  public Record2<Integer, BigDecimal> getOverdueStats(UUID teamId) {
    return dsl.select(count().as("count"), sum(PAYMENTS.AMOUNT).as("total"))
        .from(PAYMENTS)
        .where(
            PAYMENTS
                .TEAM_ID
                .eq(teamId)
                .and(PAYMENTS.STATUS.eq(PENDING.name()))
                .and(PAYMENTS.DUE_DATE.lt(LocalDate.now(clock)))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .fetchOne();
  }

  public List<Record2<String, BigDecimal>> getMonthlyPaidTrend(UUID teamId, int months) {
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
        .fetch();
  }

  public String findCurrencyByTeamId(UUID teamId) {
    return dsl.select(PAYMENTS.CURRENCY)
        .from(PAYMENTS)
        .where(PAYMENTS.TEAM_ID.eq(teamId).and(PAYMENTS.DELETED_AT.isNull()))
        .limit(1)
        .fetchOptional()
        .map(r -> r.get(PAYMENTS.CURRENCY))
        .orElse("EUR");
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(PAYMENTS)
        .set(PAYMENTS.DELETED_AT, now)
        .where(PAYMENTS.ID.eq(id).and(PAYMENTS.TEAM_ID.eq(teamId)))
        .execute();
  }
}
