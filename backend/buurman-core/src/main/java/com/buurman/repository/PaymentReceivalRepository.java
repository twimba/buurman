package com.buurman.repository;

import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.sum;
import static org.jooq.impl.DSL.table;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PaymentReceival;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.util.CurrencyUtils;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentReceivalRepository {

  private static final Table<?> TABLE = table("payment_receivals");
  private static final Field<UUID> ID = field("id", UUID.class);
  private static final Field<String> IDENTIFIER = field("identifier", String.class);
  private static final Field<UUID> TEAM_ID = field("team_id", UUID.class);
  private static final Field<UUID> PAYMENT_ID = field("payment_id", UUID.class);
  private static final Field<Long> AMOUNT = field("amount", Long.class);
  private static final Field<String> CURRENCY = field("currency", String.class);
  private static final Field<LocalDate> RECEIVAL_DATE = field("receival_date", LocalDate.class);
  private static final Field<String> NOTES = field("notes", String.class);
  private static final Field<String> RECEIVAL_TYPE = field("receival_type", String.class);
  private static final Field<UUID> CREDIT_ID = field("credit_id", UUID.class);
  private static final Field<UUID> PLAN_ID = field("payment_plan_id", UUID.class);
  private static final Field<LocalDateTime> CREATED_AT = field("created_at", LocalDateTime.class);
  private static final Field<LocalDateTime> UPDATED_AT = field("updated_at", LocalDateTime.class);
  private static final Field<UUID> CREATED_BY = field("created_by", UUID.class);
  private static final Field<UUID> UPDATED_BY = field("updated_by", UUID.class);
  private static final Field<LocalDateTime> DELETED_AT = field("deleted_at", LocalDateTime.class);

  private final DSLContext dsl;
  private final Clock clock;

  public List<PaymentReceival> findByPaymentIdAndTeamId(UUID paymentId, UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(TABLE)
            .where(PAYMENT_ID.eq(paymentId).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
            .orderBy(RECEIVAL_DATE.desc())
            .fetch()
            .map(this::toDomain));
  }

  public List<PaymentReceival> findByPaymentIdsAndTeamId(Collection<UUID> paymentIds, UUID teamId) {
    if (paymentIds == null || paymentIds.isEmpty()) {
      return List.of();
    }
    return List.copyOf(
        dsl.selectFrom(TABLE)
            .where(PAYMENT_ID.in(paymentIds).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
            .orderBy(RECEIVAL_DATE.desc())
            .fetch()
            .map(this::toDomain));
  }

  public Optional<PaymentReceival> findByIdentifierAndPaymentIdAndTeamId(
      Sid identifier, UUID paymentId, UUID teamId) {
    return dsl.selectFrom(TABLE)
        .where(
            IDENTIFIER
                .eq(identifier.value())
                .and(PAYMENT_ID.eq(paymentId))
                .and(TEAM_ID.eq(teamId))
                .and(DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public PaymentReceival getByIdentifierAndPaymentIdAndTeamId(
      Sid identifier, UUID paymentId, UUID teamId) {
    return findByIdentifierAndPaymentIdAndTeamId(identifier, paymentId, teamId)
        .orElseThrow(() -> new NotFoundException("Payment receival not found"));
  }

  public Optional<PaymentReceival> findByIdAndTeamId(UUID id, UUID teamId) {
    return dsl.selectFrom(TABLE)
        .where(ID.eq(id).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
        .fetchOptional()
        .map(this::toDomain);
  }

  public BigDecimal sumByPaymentIdAndTeamId(
      UUID paymentId, UUID teamId, @Nullable String currency) {
    BigDecimal sum =
        dsl.select(coalesce(sum(AMOUNT), 0L))
            .from(TABLE)
            .where(PAYMENT_ID.eq(paymentId).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
            .fetchOneInto(BigDecimal.class);
    return MoneyAmount.sumToMajorUnits(sum, currency);
  }

  /** Payments settled into a plan (distinct payment ids of PLAN receivals for the plan). */
  public List<UUID> findPaymentIdsByPlanId(UUID planId, UUID teamId) {
    return dsl.selectDistinct(PAYMENT_ID)
        .from(TABLE)
        .where(PLAN_ID.eq(planId).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
        .fetch(PAYMENT_ID);
  }

  public void save(PaymentReceival receival) {
    LocalDateTime now = LocalDateTime.now(clock);

    UUID id = UUID.randomUUID();

    dsl.insertInto(TABLE)
        .set(ID, id)
        .set(IDENTIFIER, receival.getIdentifier().orElseThrow().value())
        .set(TEAM_ID, receival.getTeamId())
        .set(PAYMENT_ID, receival.getPaymentId())
        .set(AMOUNT, receival.getAmount().toMinorUnits())
        .set(CURRENCY, receival.getAmount().currency())
        .set(RECEIVAL_DATE, receival.getReceivalDate())
        .set(NOTES, receival.getNotes().orElse(null))
        .set(RECEIVAL_TYPE, receival.getReceivalType().name())
        .set(CREDIT_ID, receival.getCreditId().orElse(null))
        .set(PLAN_ID, receival.getPaymentPlanId().orElse(null))
        .set(CREATED_AT, now)
        .set(UPDATED_AT, now)
        .set(CREATED_BY, receival.getCreatedBy())
        .set(UPDATED_BY, receival.getUpdatedBy())
        .execute();

    receival.setId(id);
    receival.setCreatedAt(now.toInstant(UTC));
    receival.setUpdatedAt(now.toInstant(UTC));
  }

  public void update(
      UUID id,
      UUID teamId,
      BigDecimal amount,
      LocalDate receivalDate,
      @Nullable String notes,
      UUID updatedBy,
      String currency) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(TABLE)
        .set(AMOUNT, MoneyAmount.of(amount, currency).toMinorUnits())
        .set(RECEIVAL_DATE, receivalDate)
        .set(NOTES, notes)
        .set(UPDATED_AT, now)
        .set(UPDATED_BY, updatedBy)
        .where(ID.eq(id).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
        .execute();
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(TABLE).set(DELETED_AT, now).where(ID.eq(id).and(TEAM_ID.eq(teamId))).execute();
  }

  public void softDeleteByPaymentIdAndTeamId(UUID paymentId, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(TABLE)
        .set(DELETED_AT, now)
        .where(PAYMENT_ID.eq(paymentId).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
        .execute();
  }

  private PaymentReceival toDomain(Record record) {
    String currency = record.get(CURRENCY);
    PaymentReceival receival = new PaymentReceival();
    receival.setId(record.get(ID));
    receival.setIdentifier(
        java.util.Optional.of(com.buurman.domain.Sid.of(record.get(IDENTIFIER))));
    receival.setTeamId(record.get(TEAM_ID));
    receival.setPaymentId(record.get(PAYMENT_ID));
    Long minorUnits = record.get(AMOUNT);
    int digits = CurrencyUtils.getFractionalDigits(currency);
    BigDecimal majorUnits =
        minorUnits != null ? BigDecimal.valueOf(minorUnits, digits) : BigDecimal.ZERO;
    receival.setAmount(MoneyAmount.of(majorUnits, currency));
    LocalDate receivalDate = toLocalDate(record.get("receival_date"));
    if (receivalDate != null) {
      receival.setReceivalDate(receivalDate);
    }
    receival.setNotes(Optional.ofNullable(record.get(NOTES)));
    receival.setReceivalType(
        Optional.ofNullable(record.get(RECEIVAL_TYPE))
            .map(PaymentReceival.ReceivalType::valueOf)
            .orElse(PaymentReceival.ReceivalType.PAYMENT));
    receival.setCreditId(Optional.ofNullable(record.get(CREDIT_ID)));
    receival.setPaymentPlanId(Optional.ofNullable(record.get(PLAN_ID)));
    Instant createdAt = toInstant(record.get("created_at"));
    if (createdAt != null) {
      receival.setCreatedAt(createdAt);
    }
    Instant updatedAt = toInstant(record.get("updated_at"));
    if (updatedAt != null) {
      receival.setUpdatedAt(updatedAt);
    }
    receival.setCreatedBy(record.get(CREATED_BY));
    receival.setUpdatedBy(record.get(UPDATED_BY));
    receival.setDeletedAt(Optional.ofNullable(toInstant(record.get("deleted_at"))));
    return receival;
  }

  private static @Nullable LocalDate toLocalDate(@Nullable Object val) {
    if (val instanceof LocalDate ld) {
      return ld;
    }
    if (val instanceof java.sql.Date sd) {
      return sd.toLocalDate();
    }
    return null;
  }

  private static @Nullable Instant toInstant(@Nullable Object val) {
    if (val instanceof LocalDateTime ldt) {
      return ldt.toInstant(UTC);
    }
    if (val instanceof java.sql.Timestamp ts) {
      return ts.toInstant();
    }
    return null;
  }
}
