package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTY_FEES;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PropertyFee;
import com.buurman.domain.Sid;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.PropertyFeeRecordMapper;
import com.buurman.util.CurrencyUtils;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PropertyFeeRepository {

  private final DSLContext dsl;
  private final PropertyFeeRecordMapper mapper;
  private final Clock clock;

  public Optional<PropertyFee> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(PROPERTY_FEES)
        .where(
            PROPERTY_FEES
                .IDENTIFIER
                .eq(identifier)
                .and(PROPERTY_FEES.TEAM_ID.eq(teamId))
                .and(PROPERTY_FEES.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public PropertyFee getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Property fee not found"));
  }

  public List<PropertyFee> findByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    return dsl
        .selectFrom(PROPERTY_FEES)
        .where(
            PROPERTY_FEES
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_FEES.TEAM_ID.eq(teamId))
                .and(PROPERTY_FEES.DELETED_AT.isNull()))
        .orderBy(PROPERTY_FEES.CREATED_AT.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public List<PropertyFee> findActiveByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    return dsl
        .selectFrom(PROPERTY_FEES)
        .where(
            PROPERTY_FEES
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_FEES.TEAM_ID.eq(teamId))
                .and(PROPERTY_FEES.STATUS.eq(PropertyFee.FeeStatus.ACTIVE.name()))
                .and(PROPERTY_FEES.DELETED_AT.isNull()))
        .orderBy(PROPERTY_FEES.CREATED_AT.desc())
        .fetch()
        .stream()
        .map(mapper::toDomain)
        .flatMap(Optional::stream)
        .toList();
  }

  public PropertyFee save(PropertyFee fee) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (fee.getId() == null) {
      // Insert
      UUID id = UUID.randomUUID();
      LocalDateTime createdAt =
          fee.getCreatedAt() != null ? LocalDateTime.ofInstant(fee.getCreatedAt(), UTC) : now;
      LocalDateTime updatedAt =
          fee.getUpdatedAt() != null ? LocalDateTime.ofInstant(fee.getUpdatedAt(), UTC) : now;

      String currency = fee.getCurrency();
      dsl.insertInto(PROPERTY_FEES)
          .set(PROPERTY_FEES.ID, id)
          .set(PROPERTY_FEES.IDENTIFIER, fee.getIdentifier().orElseThrow())
          .set(PROPERTY_FEES.PROPERTY_ID, fee.getPropertyId())
          .set(PROPERTY_FEES.TEAM_ID, fee.getTeamId())
          .set(PROPERTY_FEES.FEE_TYPE, fee.getFeeType().name())
          .set(PROPERTY_FEES.NAME, fee.getName().orElse(null))
          .set(
              PROPERTY_FEES.ANNUAL_AMOUNT,
              CurrencyUtils.toMinorUnits(fee.getAnnualAmount(), currency))
          .set(PROPERTY_FEES.CURRENCY, currency)
          .set(PROPERTY_FEES.PAYMENT_FREQUENCY, fee.getPaymentFrequency())
          .set(PROPERTY_FEES.DUE_MONTHS, fee.getDueMonths().orElse(null))
          .set(PROPERTY_FEES.START_DATE, fee.getStartDate().orElse(null))
          .set(PROPERTY_FEES.END_DATE, fee.getEndDate().orElse(null))
          .set(PROPERTY_FEES.STATUS, fee.getStatus().name())
          .set(PROPERTY_FEES.NOTES, fee.getNotes().orElse(null))
          .set(PROPERTY_FEES.CREATED_AT, createdAt)
          .set(PROPERTY_FEES.UPDATED_AT, updatedAt)
          .set(PROPERTY_FEES.CREATED_BY, fee.getCreatedBy())
          .set(PROPERTY_FEES.UPDATED_BY, fee.getUpdatedBy())
          .execute();

      fee.setId(id);
      fee.setCreatedAt(createdAt.toInstant(UTC));
      fee.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      // Update
      LocalDateTime updatedAt =
          fee.getUpdatedAt() != null ? LocalDateTime.ofInstant(fee.getUpdatedAt(), UTC) : now;

      String currency = fee.getCurrency();
      dsl.update(PROPERTY_FEES)
          .set(PROPERTY_FEES.FEE_TYPE, fee.getFeeType().name())
          .set(PROPERTY_FEES.NAME, fee.getName().orElse(null))
          .set(
              PROPERTY_FEES.ANNUAL_AMOUNT,
              CurrencyUtils.toMinorUnits(fee.getAnnualAmount(), currency))
          .set(PROPERTY_FEES.CURRENCY, currency)
          .set(PROPERTY_FEES.PAYMENT_FREQUENCY, fee.getPaymentFrequency())
          .set(PROPERTY_FEES.DUE_MONTHS, fee.getDueMonths().orElse(null))
          .set(PROPERTY_FEES.START_DATE, fee.getStartDate().orElse(null))
          .set(PROPERTY_FEES.END_DATE, fee.getEndDate().orElse(null))
          .set(PROPERTY_FEES.STATUS, fee.getStatus().name())
          .set(PROPERTY_FEES.NOTES, fee.getNotes().orElse(null))
          .set(PROPERTY_FEES.UPDATED_AT, updatedAt)
          .set(PROPERTY_FEES.UPDATED_BY, fee.getUpdatedBy())
          .where(PROPERTY_FEES.ID.eq(fee.getId()).and(PROPERTY_FEES.TEAM_ID.eq(fee.getTeamId())))
          .execute();

      fee.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return fee;
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(PROPERTY_FEES)
        .set(PROPERTY_FEES.DELETED_AT, now)
        .where(PROPERTY_FEES.ID.eq(id).and(PROPERTY_FEES.TEAM_ID.eq(teamId)))
        .execute();
  }
}
