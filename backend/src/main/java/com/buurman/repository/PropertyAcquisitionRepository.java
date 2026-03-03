package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTY_ACQUISITIONS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PropertyAcquisition;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.PropertyAcquisitionRecordMapper;
import com.buurman.util.CurrencyUtils;

import lombok.RequiredArgsConstructor;
import com.buurman.domain.Ulid;

@Repository
@RequiredArgsConstructor
public class PropertyAcquisitionRepository {

  private final DSLContext dsl;
  private final PropertyAcquisitionRecordMapper mapper;
  private final Clock clock;

  public Optional<PropertyAcquisition> findByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    return dsl.selectFrom(PROPERTY_ACQUISITIONS)
        .where(
            PROPERTY_ACQUISITIONS
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_ACQUISITIONS.TEAM_ID.eq(teamId))
                .and(PROPERTY_ACQUISITIONS.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public Optional<PropertyAcquisition> findByIdentifierAndTeamId(Ulid identifier, UUID teamId) {
    return dsl.selectFrom(PROPERTY_ACQUISITIONS)
        .where(
            PROPERTY_ACQUISITIONS
                .IDENTIFIER
                .eq(identifier)
                .and(PROPERTY_ACQUISITIONS.TEAM_ID.eq(teamId))
                .and(PROPERTY_ACQUISITIONS.DELETED_AT.isNull()))
        .fetchOptional()
        .flatMap(mapper::toDomain);
  }

  public PropertyAcquisition getByIdentifierAndTeamId(Ulid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Property acquisition not found"));
  }

  public PropertyAcquisition save(PropertyAcquisition acq) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (acq.getId() == null) {
      // Insert
      UUID id = UUID.randomUUID();
      LocalDateTime createdAt =
          acq.getCreatedAt() != null ? LocalDateTime.ofInstant(acq.getCreatedAt(), UTC) : now;
      LocalDateTime updatedAt =
          acq.getUpdatedAt() != null ? LocalDateTime.ofInstant(acq.getUpdatedAt(), UTC) : now;

      dsl.insertInto(PROPERTY_ACQUISITIONS)
          .set(PROPERTY_ACQUISITIONS.ID, id)
          .set(PROPERTY_ACQUISITIONS.IDENTIFIER, acq.getIdentifier().orElseThrow())
          .set(PROPERTY_ACQUISITIONS.PROPERTY_ID, acq.getPropertyId())
          .set(PROPERTY_ACQUISITIONS.TEAM_ID, acq.getTeamId())
          .set(PROPERTY_ACQUISITIONS.ACQUISITION_TYPE, acq.getAcquisitionType().name())
          .set(PROPERTY_ACQUISITIONS.ACQUISITION_DATE, acq.getAcquisitionDate().orElse(null))
          .set(
              PROPERTY_ACQUISITIONS.PURCHASE_PRICE,
              CurrencyUtils.toMinorUnitsOrNull(
                  acq.getPurchasePrice().orElse(null), acq.getPurchasePriceCurrency().orElse(null)))
          .set(
              PROPERTY_ACQUISITIONS.PURCHASE_PRICE_CURRENCY,
              acq.getPurchasePriceCurrency().orElse(null))
          .set(
              PROPERTY_ACQUISITIONS.CLOSING_COSTS,
              CurrencyUtils.toMinorUnitsOrNull(
                  acq.getClosingCosts().orElse(null), acq.getClosingCostsCurrency().orElse(null)))
          .set(
              PROPERTY_ACQUISITIONS.CLOSING_COSTS_CURRENCY,
              acq.getClosingCostsCurrency().orElse(null))
          .set(
              PROPERTY_ACQUISITIONS.RENOVATION_COSTS,
              CurrencyUtils.toMinorUnitsOrNull(
                  acq.getRenovationCosts().orElse(null),
                  acq.getRenovationCostsCurrency().orElse(null)))
          .set(
              PROPERTY_ACQUISITIONS.RENOVATION_COSTS_CURRENCY,
              acq.getRenovationCostsCurrency().orElse(null))
          .set(
              PROPERTY_ACQUISITIONS.LAND_VALUE,
              CurrencyUtils.toMinorUnitsOrNull(
                  acq.getLandValue().orElse(null), acq.getLandValueCurrency().orElse(null)))
          .set(PROPERTY_ACQUISITIONS.LAND_VALUE_CURRENCY, acq.getLandValueCurrency().orElse(null))
          .set(
              PROPERTY_ACQUISITIONS.DEPRECIATION_METHOD,
              acq.getDepreciationMethod().map(Enum::name).orElse(null))
          .set(PROPERTY_ACQUISITIONS.DEPRECIATION_YEARS, acq.getDepreciationYears().orElse(null))
          .set(PROPERTY_ACQUISITIONS.NOTES, acq.getNotes().orElse(null))
          .set(PROPERTY_ACQUISITIONS.CREATED_AT, createdAt)
          .set(PROPERTY_ACQUISITIONS.UPDATED_AT, updatedAt)
          .set(PROPERTY_ACQUISITIONS.CREATED_BY, acq.getCreatedBy())
          .set(PROPERTY_ACQUISITIONS.UPDATED_BY, acq.getUpdatedBy())
          .execute();

      acq.setId(id);
      acq.setCreatedAt(createdAt.toInstant(UTC));
      acq.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      // Update
      LocalDateTime updatedAt =
          acq.getUpdatedAt() != null ? LocalDateTime.ofInstant(acq.getUpdatedAt(), UTC) : now;

      dsl.update(PROPERTY_ACQUISITIONS)
          .set(PROPERTY_ACQUISITIONS.ACQUISITION_TYPE, acq.getAcquisitionType().name())
          .set(PROPERTY_ACQUISITIONS.ACQUISITION_DATE, acq.getAcquisitionDate().orElse(null))
          .set(
              PROPERTY_ACQUISITIONS.PURCHASE_PRICE,
              CurrencyUtils.toMinorUnitsOrNull(
                  acq.getPurchasePrice().orElse(null), acq.getPurchasePriceCurrency().orElse(null)))
          .set(
              PROPERTY_ACQUISITIONS.PURCHASE_PRICE_CURRENCY,
              acq.getPurchasePriceCurrency().orElse(null))
          .set(
              PROPERTY_ACQUISITIONS.CLOSING_COSTS,
              CurrencyUtils.toMinorUnitsOrNull(
                  acq.getClosingCosts().orElse(null), acq.getClosingCostsCurrency().orElse(null)))
          .set(
              PROPERTY_ACQUISITIONS.CLOSING_COSTS_CURRENCY,
              acq.getClosingCostsCurrency().orElse(null))
          .set(
              PROPERTY_ACQUISITIONS.RENOVATION_COSTS,
              CurrencyUtils.toMinorUnitsOrNull(
                  acq.getRenovationCosts().orElse(null),
                  acq.getRenovationCostsCurrency().orElse(null)))
          .set(
              PROPERTY_ACQUISITIONS.RENOVATION_COSTS_CURRENCY,
              acq.getRenovationCostsCurrency().orElse(null))
          .set(
              PROPERTY_ACQUISITIONS.LAND_VALUE,
              CurrencyUtils.toMinorUnitsOrNull(
                  acq.getLandValue().orElse(null), acq.getLandValueCurrency().orElse(null)))
          .set(PROPERTY_ACQUISITIONS.LAND_VALUE_CURRENCY, acq.getLandValueCurrency().orElse(null))
          .set(
              PROPERTY_ACQUISITIONS.DEPRECIATION_METHOD,
              acq.getDepreciationMethod().map(Enum::name).orElse(null))
          .set(PROPERTY_ACQUISITIONS.DEPRECIATION_YEARS, acq.getDepreciationYears().orElse(null))
          .set(PROPERTY_ACQUISITIONS.NOTES, acq.getNotes().orElse(null))
          .set(PROPERTY_ACQUISITIONS.UPDATED_AT, updatedAt)
          .set(PROPERTY_ACQUISITIONS.UPDATED_BY, acq.getUpdatedBy())
          .where(
              PROPERTY_ACQUISITIONS
                  .ID
                  .eq(acq.getId())
                  .and(PROPERTY_ACQUISITIONS.TEAM_ID.eq(acq.getTeamId())))
          .execute();

      acq.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return acq;
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(PROPERTY_ACQUISITIONS)
        .set(PROPERTY_ACQUISITIONS.DELETED_AT, now)
        .where(PROPERTY_ACQUISITIONS.ID.eq(id).and(PROPERTY_ACQUISITIONS.TEAM_ID.eq(teamId)))
        .execute();
  }
}
