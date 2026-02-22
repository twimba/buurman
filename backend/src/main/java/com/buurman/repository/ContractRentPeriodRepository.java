package com.buurman.repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.table;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Table;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.ContractRentPeriod;
import com.buurman.exception.NotFoundException;
import com.buurman.util.CurrencyUtils;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ContractRentPeriodRepository {

  private final DSLContext dsl;
  private final Clock clock;

  private static final Table<?> TABLE = table("contract_rent_periods");
  private static final Field<UUID> ID = field("id", UUID.class);
  private static final Field<String> IDENTIFIER = field("identifier", String.class);
  private static final Field<UUID> TEAM_ID = field("team_id", UUID.class);
  private static final Field<UUID> CONTRACT_ID = field("contract_id", UUID.class);
  private static final Field<Long> RENT_AMOUNT = field("rent_amount", Long.class);
  private static final Field<String> CURRENCY = field("currency", String.class);
  private static final Field<Date> EFFECTIVE_FROM = field("effective_from", Date.class);
  private static final Field<Date> EFFECTIVE_TO = field("effective_to", Date.class);
  private static final Field<String> NOTES = field("notes", String.class);
  private static final Field<Timestamp> CREATED_AT = field("created_at", Timestamp.class);
  private static final Field<Timestamp> UPDATED_AT = field("updated_at", Timestamp.class);
  private static final Field<UUID> CREATED_BY = field("created_by", UUID.class);
  private static final Field<UUID> UPDATED_BY = field("updated_by", UUID.class);
  private static final Field<Timestamp> DELETED_AT = field("deleted_at", Timestamp.class);

  public ContractRentPeriod save(ContractRentPeriod period) {
    LocalDateTime now = LocalDateTime.now(clock);
    String currency = period.getCurrency();
    Timestamp createdAt =
        period.getCreatedAt() != null
            ? Timestamp.from(period.getCreatedAt())
            : Timestamp.valueOf(now);
    Timestamp updatedAt =
        period.getUpdatedAt() != null
            ? Timestamp.from(period.getUpdatedAt())
            : Timestamp.valueOf(now);

    if (period.getId() == null) {
      UUID id = UUID.randomUUID();
      dsl.insertInto(TABLE)
          .set(ID, id)
          .set(IDENTIFIER, period.getIdentifier())
          .set(TEAM_ID, period.getTeamId())
          .set(CONTRACT_ID, period.getContractId())
          .set(RENT_AMOUNT, CurrencyUtils.toMinorUnits(period.getRentAmount(), currency))
          .set(CURRENCY, currency)
          .set(EFFECTIVE_FROM, Date.valueOf(period.getEffectiveFrom()))
          .set(
              EFFECTIVE_TO,
              period.getEffectiveTo() != null ? Date.valueOf(period.getEffectiveTo()) : null)
          .set(NOTES, period.getNotes())
          .set(CREATED_AT, createdAt)
          .set(UPDATED_AT, updatedAt)
          .set(CREATED_BY, period.getCreatedBy())
          .set(UPDATED_BY, period.getUpdatedBy())
          .execute();

      period.setId(id);
      period.setCreatedAt(createdAt.toInstant());
      period.setUpdatedAt(updatedAt.toInstant());
    } else {
      dsl.update(TABLE)
          .set(RENT_AMOUNT, CurrencyUtils.toMinorUnits(period.getRentAmount(), currency))
          .set(EFFECTIVE_FROM, Date.valueOf(period.getEffectiveFrom()))
          .set(
              EFFECTIVE_TO,
              period.getEffectiveTo() != null ? Date.valueOf(period.getEffectiveTo()) : null)
          .set(NOTES, period.getNotes())
          .set(UPDATED_AT, updatedAt)
          .set(UPDATED_BY, period.getUpdatedBy())
          .where(ID.eq(period.getId()).and(TEAM_ID.eq(period.getTeamId())).and(DELETED_AT.isNull()))
          .execute();

      period.setUpdatedAt(updatedAt.toInstant());
    }
    return period;
  }

  public List<ContractRentPeriod> findByContractIdAndTeamId(UUID contractId, UUID teamId) {
    return List.copyOf(
        dsl.select()
            .from(TABLE)
            .where(CONTRACT_ID.eq(contractId).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
            .orderBy(EFFECTIVE_FROM.desc())
            .fetch(this::toDomain));
  }

  public Optional<ContractRentPeriod> findByIdentifierAndTeamId(String identifier, UUID teamId) {
    return dsl.select()
        .from(TABLE)
        .where(IDENTIFIER.eq(identifier).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
        .fetchOptional(this::toDomain);
  }

  public ContractRentPeriod getByIdentifierAndTeamId(String identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Rent period not found"));
  }

  public Optional<ContractRentPeriod> findCurrentByContractIdAndTeamId(
      UUID contractId, UUID teamId) {
    LocalDate today = LocalDate.now(clock);
    return dsl.select()
        .from(TABLE)
        .where(
            CONTRACT_ID
                .eq(contractId)
                .and(TEAM_ID.eq(teamId))
                .and(EFFECTIVE_FROM.le(Date.valueOf(today)))
                .and(EFFECTIVE_TO.isNull().or(EFFECTIVE_TO.ge(Date.valueOf(today))))
                .and(DELETED_AT.isNull()))
        .orderBy(EFFECTIVE_FROM.desc())
        .limit(1)
        .fetchOptional(this::toDomain);
  }

  public Optional<ContractRentPeriod> findAtDateByContractIdAndTeamId(
      UUID contractId, UUID teamId, LocalDate date) {
    return dsl.select()
        .from(TABLE)
        .where(
            CONTRACT_ID
                .eq(contractId)
                .and(TEAM_ID.eq(teamId))
                .and(EFFECTIVE_FROM.le(Date.valueOf(date)))
                .and(EFFECTIVE_TO.isNull().or(EFFECTIVE_TO.ge(Date.valueOf(date))))
                .and(DELETED_AT.isNull()))
        .orderBy(EFFECTIVE_FROM.desc())
        .limit(1)
        .fetchOptional(this::toDomain);
  }

  public Optional<ContractRentPeriod> findPreviousPeriod(
      UUID contractId, UUID teamId, LocalDate beforeDate) {
    return dsl.select()
        .from(TABLE)
        .where(
            CONTRACT_ID
                .eq(contractId)
                .and(TEAM_ID.eq(teamId))
                .and(EFFECTIVE_FROM.lt(Date.valueOf(beforeDate)))
                .and(DELETED_AT.isNull()))
        .orderBy(EFFECTIVE_FROM.desc())
        .limit(1)
        .fetchOptional(this::toDomain);
  }

  public void setEffectiveTo(UUID periodId, UUID teamId, @Nullable LocalDate effectiveTo) {
    Timestamp now = Timestamp.valueOf(LocalDateTime.now(clock));
    dsl.update(TABLE)
        .set(EFFECTIVE_TO, effectiveTo != null ? Date.valueOf(effectiveTo) : null)
        .set(UPDATED_AT, now)
        .where(ID.eq(periodId).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
        .execute();
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    Timestamp now = Timestamp.valueOf(LocalDateTime.now(clock));
    dsl.update(TABLE)
        .set(DELETED_AT, now)
        .where(ID.eq(id).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
        .execute();
  }

  public void softDeleteByContractIdAndTeamId(UUID contractId, UUID teamId) {
    Timestamp now = Timestamp.valueOf(LocalDateTime.now(clock));
    dsl.update(TABLE)
        .set(DELETED_AT, now)
        .where(CONTRACT_ID.eq(contractId).and(TEAM_ID.eq(teamId)).and(DELETED_AT.isNull()))
        .execute();
  }

  private ContractRentPeriod toDomain(Record record) {
    String currency = record.get(CURRENCY);
    ContractRentPeriod period = new ContractRentPeriod();
    period.setId(record.get(ID));
    period.setIdentifier(record.get(IDENTIFIER));
    period.setTeamId(record.get(TEAM_ID));
    period.setContractId(record.get(CONTRACT_ID));
    period.setRentAmount(CurrencyUtils.toMajorUnits(record.get(RENT_AMOUNT), currency));
    period.setCurrency(currency);

    Date effectiveFromVal = record.get(EFFECTIVE_FROM);
    if (effectiveFromVal != null) {
      period.setEffectiveFrom(effectiveFromVal.toLocalDate());
    }

    Date effectiveToVal = record.get(EFFECTIVE_TO);
    period.setEffectiveTo(effectiveToVal != null ? effectiveToVal.toLocalDate() : null);

    period.setNotes(record.get(NOTES));

    Timestamp createdAtVal = record.get(CREATED_AT);
    if (createdAtVal != null) {
      period.setCreatedAt(createdAtVal.toInstant());
    }

    Timestamp updatedAtVal = record.get(UPDATED_AT);
    if (updatedAtVal != null) {
      period.setUpdatedAt(updatedAtVal.toInstant());
    }

    period.setCreatedBy(record.get(CREATED_BY));
    period.setUpdatedBy(record.get(UPDATED_BY));

    Timestamp deletedAtVal = record.get(DELETED_AT);
    period.setDeletedAt(deletedAtVal != null ? deletedAtVal.toInstant() : null);

    return period;
  }
}
