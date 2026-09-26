package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.UNITS;
import static java.time.ZoneOffset.UTC;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Sid;
import com.buurman.domain.Unit;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.UnitRecordMapper;
import com.buurman.util.MoneyAmount;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UnitRepository {

  private final DSLContext dsl;
  private final UnitRecordMapper mapper;
  private final Clock clock;

  // No PROPERTIES join here (unlike the listing/count methods below): a single-row lookup by
  // identifier returns the unit as-is regardless of its parent property's soft-delete state,
  // matching PropertyRepository's identifier-lookup pattern. Callers that must not resolve units
  // of a soft-deleted property use the joined listing/count methods instead.
  public Optional<Unit> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return dsl.selectFrom(UNITS)
        .where(
            UNITS
                .IDENTIFIER
                .eq(identifier)
                .and(UNITS.TEAM_ID.eq(teamId))
                .and(UNITS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public Unit getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Unit not found"));
  }

  // Same no-PROPERTIES-join rationale as findByIdentifierAndTeamId above: a single-row lookup by
  // internal id is used to resolve an already-validated foreign key (e.g. contracts.unit_id), not
  // to decide client-facing visibility.
  public Optional<Unit> findByIdAndTeamId(UUID id, UUID teamId) {
    return dsl.selectFrom(UNITS)
        .where(UNITS.ID.eq(id).and(UNITS.TEAM_ID.eq(teamId)).and(UNITS.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public Unit getByIdAndTeamId(UUID id, UUID teamId) {
    return findByIdAndTeamId(id, teamId).orElseThrow(() -> new NotFoundException("Unit not found"));
  }

  public List<Unit> findByIdsAndTeamId(Collection<UUID> ids, UUID teamId) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return List.copyOf(
        dsl.selectFrom(UNITS)
            .where(UNITS.ID.in(ids).and(UNITS.TEAM_ID.eq(teamId)).and(UNITS.DELETED_AT.isNull()))
            .fetch()
            .map(mapper::toDomain));
  }

  public List<Unit> findAllByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    return List.copyOf(
        dsl.select(UNITS.fields())
            .from(UNITS)
            .join(PROPERTIES)
            .on(PROPERTIES.ID.eq(UNITS.PROPERTY_ID))
            .where(
                UNITS
                    .PROPERTY_ID
                    .eq(propertyId)
                    .and(UNITS.TEAM_ID.eq(teamId))
                    .and(UNITS.DELETED_AT.isNull())
                    .and(PROPERTIES.DELETED_AT.isNull()))
            .orderBy(UNITS.SORT_ORDER.asc(), UNITS.UNIT_NUMBER.asc())
            .fetchInto(UNITS)
            .map(mapper::toDomain));
  }

  public int countActiveByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    Integer count =
        dsl.selectCount()
            .from(UNITS)
            .join(PROPERTIES)
            .on(PROPERTIES.ID.eq(UNITS.PROPERTY_ID))
            .where(
                UNITS
                    .PROPERTY_ID
                    .eq(propertyId)
                    .and(UNITS.TEAM_ID.eq(teamId))
                    .and(UNITS.DELETED_AT.isNull())
                    .and(PROPERTIES.DELETED_AT.isNull()))
            .fetchOne(0, Integer.class);
    return count == null ? 0 : count;
  }

  public int countActiveByTeamId(UUID teamId) {
    Integer count =
        dsl.selectCount()
            .from(UNITS)
            .join(PROPERTIES)
            .on(PROPERTIES.ID.eq(UNITS.PROPERTY_ID))
            .where(
                UNITS
                    .TEAM_ID
                    .eq(teamId)
                    .and(UNITS.DELETED_AT.isNull())
                    .and(PROPERTIES.DELETED_AT.isNull()))
            .fetchOne(0, Integer.class);
    return count == null ? 0 : count;
  }

  public Unit save(Unit unit) {
    LocalDateTime now = LocalDateTime.now(clock);
    BigDecimal wozValueAmount = unit.getWozValue().map(MoneyAmount::value).orElse(null);
    String wozValueCurrency = unit.getWozValue().map(MoneyAmount::currency).orElse(null);

    if (unit.getId() == null) {
      // INSERT
      UUID newId = UUID.randomUUID();
      LocalDateTime createdAt =
          unit.getCreatedAt().map(instant -> LocalDateTime.ofInstant(instant, UTC)).orElse(now);
      LocalDateTime updatedAt =
          unit.getUpdatedAt().map(instant -> LocalDateTime.ofInstant(instant, UTC)).orElse(now);

      dsl.insertInto(UNITS)
          .set(UNITS.ID, newId)
          .set(UNITS.IDENTIFIER, unit.getIdentifier().orElseThrow())
          .set(UNITS.TEAM_ID, unit.getTeamId())
          .set(UNITS.PROPERTY_ID, unit.getPropertyId())
          .set(UNITS.NAME, unit.getName().orElse(null))
          .set(UNITS.UNIT_NUMBER, unit.getUnitNumber())
          .set(UNITS.FLOOR, unit.getFloor().orElse(null))
          .set(UNITS.SORT_ORDER, unit.getSortOrder())
          .set(UNITS.UNIT_TYPE, unit.getUnitType().name())
          .set(UNITS.STATUS, unit.getStatus().name())
          .set(UNITS.IS_IMPLICIT, unit.isImplicit())
          .set(UNITS.WOZ_VALUE, wozValueAmount)
          .set(UNITS.WOZ_VALUE_CURRENCY, wozValueCurrency)
          .set(UNITS.WOZ_SHARE_PCT, unit.getWozSharePct().orElse(null))
          .set(UNITS.ALLOCATION_SHARE, unit.getAllocationShare().orElse(null))
          .set(UNITS.AREA_VALUE, unit.getAreaValue().orElse(null))
          .set(UNITS.AREA_UNIT, unit.getAreaUnit().orElse("sqm"))
          .set(UNITS.ENERGY_EFFICIENCY_RATING, unit.getEnergyEfficiencyRating().orElse(null))
          .set(
              UNITS.ENERGY_CERTIFICATE_EXPIRY_DATE,
              unit.getEnergyCertificateExpiryDate().orElse(null))
          .set(UNITS.HEATING_TYPE, unit.getHeatingType().orElse(null))
          .set(UNITS.COOLING_TYPE, unit.getCoolingType().orElse(null))
          .set(UNITS.HOT_WATER_SYSTEM, unit.getHotWaterSystem().orElse(null))
          .set(UNITS.INSULATION_NOTES, unit.getInsulationNotes().orElse(null))
          .set(UNITS.FLOORING_TYPE, unit.getFlooringType().orElse(null))
          .set(UNITS.WINDOW_TYPE, unit.getWindowType().orElse(null))
          .set(UNITS.HAS_SMOKE_DETECTORS, unit.getHasSmokeDetectors().orElse(null))
          .set(UNITS.HAS_CO_DETECTORS, unit.getHasCoDetectors().orElse(null))
          .set(UNITS.HAS_FIRE_EXTINGUISHER, unit.getHasFireExtinguisher().orElse(null))
          .set(UNITS.HAS_ADAPTED_BATHROOM, unit.getHasAdaptedBathroom().orElse(null))
          .set(UNITS.ACCESSIBILITY_NOTES, unit.getAccessibilityNotes().orElse(null))
          .set(UNITS.CREATED_AT, createdAt)
          .set(UNITS.UPDATED_AT, updatedAt)
          .set(UNITS.CREATED_BY, unit.getCreatedBy().orElse(null))
          .set(UNITS.UPDATED_BY, unit.getUpdatedBy().orElse(null))
          .execute();

      unit.setId(newId);
      unit.setCreatedAt(Optional.of(createdAt.toInstant(UTC)));
      unit.setUpdatedAt(Optional.of(updatedAt.toInstant(UTC)));
    } else {
      // UPDATE
      LocalDateTime updatedAt =
          unit.getUpdatedAt().map(instant -> LocalDateTime.ofInstant(instant, UTC)).orElse(now);

      dsl.update(UNITS)
          .set(UNITS.PROPERTY_ID, unit.getPropertyId())
          .set(UNITS.NAME, unit.getName().orElse(null))
          .set(UNITS.UNIT_NUMBER, unit.getUnitNumber())
          .set(UNITS.FLOOR, unit.getFloor().orElse(null))
          .set(UNITS.SORT_ORDER, unit.getSortOrder())
          .set(UNITS.UNIT_TYPE, unit.getUnitType().name())
          .set(UNITS.STATUS, unit.getStatus().name())
          .set(UNITS.IS_IMPLICIT, unit.isImplicit())
          .set(UNITS.WOZ_VALUE, wozValueAmount)
          .set(UNITS.WOZ_VALUE_CURRENCY, wozValueCurrency)
          .set(UNITS.WOZ_SHARE_PCT, unit.getWozSharePct().orElse(null))
          .set(UNITS.ALLOCATION_SHARE, unit.getAllocationShare().orElse(null))
          .set(UNITS.AREA_VALUE, unit.getAreaValue().orElse(null))
          .set(UNITS.AREA_UNIT, unit.getAreaUnit().orElse("sqm"))
          .set(UNITS.ENERGY_EFFICIENCY_RATING, unit.getEnergyEfficiencyRating().orElse(null))
          .set(
              UNITS.ENERGY_CERTIFICATE_EXPIRY_DATE,
              unit.getEnergyCertificateExpiryDate().orElse(null))
          .set(UNITS.HEATING_TYPE, unit.getHeatingType().orElse(null))
          .set(UNITS.COOLING_TYPE, unit.getCoolingType().orElse(null))
          .set(UNITS.HOT_WATER_SYSTEM, unit.getHotWaterSystem().orElse(null))
          .set(UNITS.INSULATION_NOTES, unit.getInsulationNotes().orElse(null))
          .set(UNITS.FLOORING_TYPE, unit.getFlooringType().orElse(null))
          .set(UNITS.WINDOW_TYPE, unit.getWindowType().orElse(null))
          .set(UNITS.HAS_SMOKE_DETECTORS, unit.getHasSmokeDetectors().orElse(null))
          .set(UNITS.HAS_CO_DETECTORS, unit.getHasCoDetectors().orElse(null))
          .set(UNITS.HAS_FIRE_EXTINGUISHER, unit.getHasFireExtinguisher().orElse(null))
          .set(UNITS.HAS_ADAPTED_BATHROOM, unit.getHasAdaptedBathroom().orElse(null))
          .set(UNITS.ACCESSIBILITY_NOTES, unit.getAccessibilityNotes().orElse(null))
          .set(UNITS.UPDATED_AT, updatedAt)
          .set(UNITS.UPDATED_BY, unit.getUpdatedBy().orElse(null))
          .where(UNITS.ID.eq(unit.getId()).and(UNITS.TEAM_ID.eq(unit.getTeamId())))
          .execute();

      unit.setUpdatedAt(Optional.of(updatedAt.toInstant(UTC)));
    }

    return unit;
  }

  public void softDelete(UUID unitId, UUID teamId, UUID actorId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(UNITS)
        .set(UNITS.DELETED_AT, now)
        .set(UNITS.UPDATED_BY, actorId)
        .where(UNITS.ID.eq(unitId).and(UNITS.TEAM_ID.eq(teamId)))
        .execute();
  }
}
