package com.buurman.repository;

import com.buurman.domain.Payment;
import com.buurman.mapper.PaymentRecordMapper;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.PAYMENTS;

@Repository
public class PaymentRepository {

    private final DSLContext dsl;
    private final PaymentRecordMapper mapper;

    public PaymentRepository(DSLContext dsl, PaymentRecordMapper mapper) {
        this.dsl = dsl;
        this.mapper = mapper;
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
                        .and(PAYMENTS.STATUS.eq(Payment.PaymentStatus.PENDING.name()))
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

    public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(PAYMENTS)
                .set(PAYMENTS.DELETED_AT, now)
                .where(PAYMENTS.ID.eq(id)
                        .and(PAYMENTS.TEAM_ID.eq(teamId)))
                .execute();
    }
}
