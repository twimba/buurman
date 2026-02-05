package com.buurman.repository;

import com.buurman.domain.PaymentReceival;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentReceivalRepository {

    private static final Table<?> TABLE = DSL.table("payment_receivals");
    private static final Field<UUID> ID = DSL.field("id", UUID.class);
    private static final Field<String> IDENTIFIER = DSL.field("identifier", String.class);
    private static final Field<UUID> TEAM_ID = DSL.field("team_id", UUID.class);
    private static final Field<UUID> PAYMENT_ID = DSL.field("payment_id", UUID.class);
    private static final Field<BigDecimal> AMOUNT = DSL.field("amount", BigDecimal.class);
    private static final Field<LocalDate> RECEIVAL_DATE = DSL.field("receival_date", LocalDate.class);
    private static final Field<String> NOTES = DSL.field("notes", String.class);
    private static final Field<LocalDateTime> CREATED_AT = DSL.field("created_at", LocalDateTime.class);
    private static final Field<LocalDateTime> UPDATED_AT = DSL.field("updated_at", LocalDateTime.class);
    private static final Field<UUID> CREATED_BY = DSL.field("created_by", UUID.class);
    private static final Field<UUID> UPDATED_BY = DSL.field("updated_by", UUID.class);
    private static final Field<LocalDateTime> DELETED_AT = DSL.field("deleted_at", LocalDateTime.class);

    private final DSLContext dsl;

    public PaymentReceivalRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    public List<PaymentReceival> findByPaymentIdAndTeamId(UUID paymentId, UUID teamId) {
        return dsl.selectFrom(TABLE)
                .where(PAYMENT_ID.eq(paymentId)
                        .and(TEAM_ID.eq(teamId))
                        .and(DELETED_AT.isNull()))
                .orderBy(RECEIVAL_DATE.desc())
                .fetch()
                .map(this::toDomain);
    }

    public List<PaymentReceival> findByPaymentIdsAndTeamId(Collection<UUID> paymentIds, UUID teamId) {
        if (paymentIds == null || paymentIds.isEmpty()) {
            return List.of();
        }
        return dsl.selectFrom(TABLE)
                .where(PAYMENT_ID.in(paymentIds)
                        .and(TEAM_ID.eq(teamId))
                        .and(DELETED_AT.isNull()))
                .orderBy(RECEIVAL_DATE.desc())
                .fetch()
                .map(this::toDomain);
    }

    public Optional<PaymentReceival> findByIdentifierAndPaymentIdAndTeamId(String identifier, UUID paymentId, UUID teamId) {
        return dsl.selectFrom(TABLE)
                .where(IDENTIFIER.eq(identifier)
                        .and(PAYMENT_ID.eq(paymentId))
                        .and(TEAM_ID.eq(teamId))
                        .and(DELETED_AT.isNull()))
                .fetchOptional()
                .map(this::toDomain);
    }

    public Optional<PaymentReceival> findByIdAndTeamId(UUID id, UUID teamId) {
        return dsl.selectFrom(TABLE)
                .where(ID.eq(id)
                        .and(TEAM_ID.eq(teamId))
                        .and(DELETED_AT.isNull()))
                .fetchOptional()
                .map(this::toDomain);
    }

    public BigDecimal sumByPaymentIdAndTeamId(UUID paymentId, UUID teamId) {
        BigDecimal sum = dsl.select(DSL.coalesce(DSL.sum(AMOUNT), BigDecimal.ZERO))
                .from(TABLE)
                .where(PAYMENT_ID.eq(paymentId)
                        .and(TEAM_ID.eq(teamId))
                        .and(DELETED_AT.isNull()))
                .fetchOneInto(BigDecimal.class);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    public PaymentReceival save(PaymentReceival receival) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        UUID id = UUID.randomUUID();
        LocalDateTime createdAt = receival.getCreatedAt() != null
                ? LocalDateTime.ofInstant(receival.getCreatedAt(), ZoneOffset.UTC)
                : now;
        LocalDateTime updatedAt = receival.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(receival.getUpdatedAt(), ZoneOffset.UTC)
                : now;

        dsl.insertInto(TABLE)
                .set(ID, id)
                .set(IDENTIFIER, receival.getIdentifier())
                .set(TEAM_ID, receival.getTeamId())
                .set(PAYMENT_ID, receival.getPaymentId())
                .set(AMOUNT, receival.getAmount())
                .set(RECEIVAL_DATE, receival.getReceivalDate())
                .set(NOTES, receival.getNotes())
                .set(CREATED_AT, createdAt)
                .set(UPDATED_AT, updatedAt)
                .set(CREATED_BY, receival.getCreatedBy())
                .set(UPDATED_BY, receival.getUpdatedBy())
                .execute();

        receival.setId(id);
        receival.setCreatedAt(createdAt.toInstant(ZoneOffset.UTC));
        receival.setUpdatedAt(updatedAt.toInstant(ZoneOffset.UTC));

        return receival;
    }

    public void update(UUID id, UUID teamId, BigDecimal amount, LocalDate receivalDate, String notes, UUID updatedBy) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(TABLE)
                .set(AMOUNT, amount)
                .set(RECEIVAL_DATE, receivalDate)
                .set(NOTES, notes)
                .set(UPDATED_AT, now)
                .set(UPDATED_BY, updatedBy)
                .where(ID.eq(id)
                        .and(TEAM_ID.eq(teamId))
                        .and(DELETED_AT.isNull()))
                .execute();
    }

    public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
        dsl.update(TABLE)
                .set(DELETED_AT, now)
                .where(ID.eq(id)
                        .and(TEAM_ID.eq(teamId)))
                .execute();
    }

    private PaymentReceival toDomain(Record record) {
        PaymentReceival receival = new PaymentReceival();
        receival.setId(record.get(ID));
        receival.setIdentifier(record.get(IDENTIFIER));
        receival.setTeamId(record.get(TEAM_ID));
        receival.setPaymentId(record.get(PAYMENT_ID));
        receival.setAmount(record.get(AMOUNT));
        receival.setReceivalDate(toLocalDate(record.get("receival_date")));
        receival.setNotes(record.get(NOTES));
        receival.setCreatedAt(toInstant(record.get("created_at")));
        receival.setUpdatedAt(toInstant(record.get("updated_at")));
        receival.setCreatedBy(record.get(CREATED_BY));
        receival.setUpdatedBy(record.get(UPDATED_BY));
        receival.setDeletedAt(toInstant(record.get("deleted_at")));
        return receival;
    }

    private static LocalDate toLocalDate(Object val) {
        if (val instanceof LocalDate ld) return ld;
        if (val instanceof java.sql.Date sd) return sd.toLocalDate();
        return null;
    }

    private static Instant toInstant(Object val) {
        if (val instanceof LocalDateTime ldt) return ldt.toInstant(ZoneOffset.UTC);
        if (val instanceof java.sql.Timestamp ts) return ts.toInstant();
        return null;
    }
}
