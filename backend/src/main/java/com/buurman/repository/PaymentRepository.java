package com.buurman.repository;

import com.buurman.domain.Payment;
import com.buurman.dto.request.PageRequest;
import com.buurman.mapper.PaymentRecordMapper;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record2;
import org.springframework.stereotype.Repository;

import com.buurman.jooq.generated.tables.records.PaymentsRecord;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.domain.Payment.PaymentStatus.*;
import static com.buurman.jooq.generated.Tables.PAYMENTS;
import static org.jooq.impl.DSL.*;

@Repository
public class PaymentRepository {

    private final DSLContext dsl;
    private final PaymentRecordMapper mapper;

    public PaymentRepository(DSLContext dsl, PaymentRecordMapper mapper) {
        this.dsl = dsl;
        this.mapper = mapper;
    }

    public Optional<Payment> findByIdentifierAndTeamId(String identifier, UUID teamId) {
        return dsl.selectFrom(PAYMENTS)
                .where(PAYMENTS.IDENTIFIER.eq(identifier)
                        .and(PAYMENTS.TEAM_ID.eq(teamId))
                        .and(PAYMENTS.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public Optional<Payment> findByIdAndTeamId(UUID id, UUID teamId) {
        return dsl.selectFrom(PAYMENTS)
                .where(PAYMENTS.ID.eq(id)
                        .and(PAYMENTS.TEAM_ID.eq(teamId))
                        .and(PAYMENTS.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public List<Payment> findAllByTeamId(UUID teamId) {
        return dsl.selectFrom(PAYMENTS)
                .where(PAYMENTS.TEAM_ID.eq(teamId)
                        .and(PAYMENTS.DELETED_AT.isNull()))
                .orderBy(PAYMENTS.DUE_DATE.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public List<Payment> findByContractId(UUID contractId, UUID teamId) {
        return dsl.selectFrom(PAYMENTS)
                .where(PAYMENTS.CONTRACT_ID.eq(contractId)
                        .and(PAYMENTS.TEAM_ID.eq(teamId))
                        .and(PAYMENTS.DELETED_AT.isNull()))
                .orderBy(PAYMENTS.DUE_DATE.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public List<Payment> findByStatus(Payment.PaymentStatus status, UUID teamId) {
        return dsl.selectFrom(PAYMENTS)
                .where(PAYMENTS.STATUS.eq(status.name())
                        .and(PAYMENTS.TEAM_ID.eq(teamId))
                        .and(PAYMENTS.DELETED_AT.isNull()))
                .orderBy(PAYMENTS.DUE_DATE.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public List<Payment> findOverduePayments(UUID teamId) {
        LocalDate today = LocalDate.now();
        return dsl.selectFrom(PAYMENTS)
                .where(PAYMENTS.TEAM_ID.eq(teamId)
                        .and(PAYMENTS.STATUS.eq(PENDING.name()))
                        .and(PAYMENTS.DUE_DATE.lt(today))
                        .and(PAYMENTS.DELETED_AT.isNull()))
                .orderBy(PAYMENTS.DUE_DATE.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    public List<Payment> findByDueDateRange(LocalDate startDate, LocalDate endDate, UUID teamId) {
        return dsl.selectFrom(PAYMENTS)
                .where(PAYMENTS.TEAM_ID.eq(teamId)
                        .and(PAYMENTS.DUE_DATE.between(startDate, endDate))
                        .and(PAYMENTS.DELETED_AT.isNull()))
                .orderBy(PAYMENTS.DUE_DATE.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    public List<Payment> findByDateRange(LocalDate startDate, LocalDate endDate, UUID teamId) {
        return dsl.selectFrom(PAYMENTS)
                .where(PAYMENTS.TEAM_ID.eq(teamId)
                        .and(PAYMENTS.PAYMENT_DATE.isNotNull())
                        .and(PAYMENTS.PAYMENT_DATE.between(startDate, endDate))
                        .and(PAYMENTS.DELETED_AT.isNull()))
                .orderBy(PAYMENTS.PAYMENT_DATE.asc())
                .fetch()
                .map(mapper::toDomain);
    }

    public Payment save(Payment payment) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        if (payment.getId() == null) {
            // Insert
            UUID id = UUID.randomUUID();
            LocalDateTime createdAt = payment.getCreatedAt() != null
                    ? LocalDateTime.ofInstant(payment.getCreatedAt(), ZoneOffset.UTC)
                    : now;
            LocalDateTime updatedAt = payment.getUpdatedAt() != null
                    ? LocalDateTime.ofInstant(payment.getUpdatedAt(), ZoneOffset.UTC)
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
                    .set(PAYMENTS.AUTO_GENERATED, payment.getAutoGenerated() != null ? payment.getAutoGenerated() : false)
                    .set(PAYMENTS.CREATED_AT, createdAt)
                    .set(PAYMENTS.UPDATED_AT, updatedAt)
                    .set(PAYMENTS.CREATED_BY, payment.getCreatedBy())
                    .set(PAYMENTS.UPDATED_BY, payment.getUpdatedBy())
                    .execute();

            payment.setId(id);
            payment.setCreatedAt(createdAt.toInstant(ZoneOffset.UTC));
            payment.setUpdatedAt(updatedAt.toInstant(ZoneOffset.UTC));
        } else {
            // Update
            LocalDateTime updatedAt = payment.getUpdatedAt() != null
                    ? LocalDateTime.ofInstant(payment.getUpdatedAt(), ZoneOffset.UTC)
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
                    .where(PAYMENTS.ID.eq(payment.getId())
                            .and(PAYMENTS.TEAM_ID.eq(payment.getTeamId())))
                    .execute();

            payment.setUpdatedAt(updatedAt.toInstant(ZoneOffset.UTC));
        }

        return payment;
    }

    public boolean existsByContractIdAndDueDate(UUID contractId, LocalDate dueDate) {
        return dsl.fetchExists(
                dsl.selectFrom(PAYMENTS)
                        .where(PAYMENTS.CONTRACT_ID.eq(contractId)
                                .and(PAYMENTS.DUE_DATE.eq(dueDate))
                                .and(PAYMENTS.DELETED_AT.isNull()))
        );
    }

    public List<Payment> findFuturePendingByContractId(UUID contractId, UUID teamId) {
        LocalDate today = LocalDate.now();
        return dsl.selectFrom(PAYMENTS)
                .where(PAYMENTS.CONTRACT_ID.eq(contractId)
                        .and(PAYMENTS.TEAM_ID.eq(teamId))
                        .and(PAYMENTS.STATUS.eq(PENDING.name()))
                        .and(PAYMENTS.DUE_DATE.gt(today))
                        .and(PAYMENTS.DELETED_AT.isNull()))
                .fetch()
                .map(mapper::toDomain);
    }

    public PaginatedResult<Payment> findAllByTeamIdPaginated(UUID teamId, String status, UUID contractId, PageRequest pageRequest) {
        Condition condition = PAYMENTS.TEAM_ID.eq(teamId).and(PAYMENTS.DELETED_AT.isNull());
        if (status != null && !status.isEmpty()) {
            if ("OVERDUE".equalsIgnoreCase(status)) {
                condition = condition.and(PAYMENTS.STATUS.eq(PENDING.name()))
                        .and(PAYMENTS.DUE_DATE.lt(LocalDate.now()));
            } else {
                condition = condition.and(PAYMENTS.STATUS.eq(status));
            }
        }
        if (contractId != null) {
            condition = condition.and(PAYMENTS.CONTRACT_ID.eq(contractId));
        }
        Map<String, Field<?>> sortableFields = Map.of(
            "dueDate", PAYMENTS.DUE_DATE,
            "amount", PAYMENTS.AMOUNT,
            "status", PAYMENTS.STATUS,
            "createdAt", PAYMENTS.CREATED_AT
        );
        return PaginationHelper.paginate(dsl, PAYMENTS, condition, sortableFields, PAYMENTS.DUE_DATE, pageRequest, r -> mapper.toDomain((PaymentsRecord) r));
    }

    public Record2<Integer, BigDecimal> getPendingStats(UUID teamId) {
        return dsl.select(
                count().as("count"),
                sum(PAYMENTS.AMOUNT).as("total")
        )
        .from(PAYMENTS)
        .where(PAYMENTS.TEAM_ID.eq(teamId)
                .and(PAYMENTS.STATUS.in(PENDING.name(), PARTIALLY_PAID.name()))
                .and(PAYMENTS.DUE_DATE.ge(LocalDate.now()))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .fetchOne();
    }

    public Record2<Integer, BigDecimal> getOverdueStats(UUID teamId) {
        return dsl.select(
                count().as("count"),
                sum(PAYMENTS.AMOUNT).as("total")
        )
        .from(PAYMENTS)
        .where(PAYMENTS.TEAM_ID.eq(teamId)
                .and(PAYMENTS.STATUS.eq(PENDING.name()))
                .and(PAYMENTS.DUE_DATE.lt(LocalDate.now()))
                .and(PAYMENTS.DELETED_AT.isNull()))
        .fetchOne();
    }

    public List<Record2<String, BigDecimal>> getMonthlyPaidTrend(UUID teamId, int months) {
        LocalDate startDate = LocalDate.now().minusMonths(months).withDayOfMonth(1);
        return dsl.select(
                field("to_char({0}, 'YYYY-MM')", String.class, PAYMENTS.PAYMENT_DATE).as("month"),
                sum(PAYMENTS.AMOUNT).as("total")
        )
        .from(PAYMENTS)
        .where(PAYMENTS.TEAM_ID.eq(teamId)
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
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(PAYMENTS)
                .set(PAYMENTS.DELETED_AT, now)
                .where(PAYMENTS.ID.eq(id)
                        .and(PAYMENTS.TEAM_ID.eq(teamId)))
                .execute();
    }
}
