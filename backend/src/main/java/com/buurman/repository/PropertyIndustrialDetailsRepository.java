package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTY_INDUSTRIAL_DETAILS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PropertyIndustrialDetails;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PropertyIndustrialDetailsRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public Optional<PropertyIndustrialDetails> findByPropertyIdAndTeamId(
      UUID propertyId, UUID teamId) {
    return dsl.selectFrom(PROPERTY_INDUSTRIAL_DETAILS)
        .where(
            PROPERTY_INDUSTRIAL_DETAILS
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_INDUSTRIAL_DETAILS.TEAM_ID.eq(teamId)))
        .fetchOptional()
        .map(this::toDomain);
  }

  public PropertyIndustrialDetails save(PropertyIndustrialDetails details) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (details.getId() == null) {
      UUID newId = UUID.randomUUID();
      dsl.insertInto(PROPERTY_INDUSTRIAL_DETAILS)
          .set(PROPERTY_INDUSTRIAL_DETAILS.ID, newId)
          .set(PROPERTY_INDUSTRIAL_DETAILS.PROPERTY_ID, details.getPropertyId())
          .set(PROPERTY_INDUSTRIAL_DETAILS.TEAM_ID, details.getTeamId())
          .set(PROPERTY_INDUSTRIAL_DETAILS.CLEAR_HEIGHT_M, details.getClearHeightM())
          .set(PROPERTY_INDUSTRIAL_DETAILS.LOADING_DOCKS, details.getLoadingDocks())
          .set(PROPERTY_INDUSTRIAL_DETAILS.DRIVE_IN_DOORS, details.getDriveInDoors())
          .set(
              PROPERTY_INDUSTRIAL_DETAILS.FLOOR_LOAD_CAPACITY_KG_SQM,
              details.getFloorLoadCapacityKgSqm())
          .set(PROPERTY_INDUSTRIAL_DETAILS.POWER_CAPACITY_KVA, details.getPowerCapacityKva())
          .set(PROPERTY_INDUSTRIAL_DETAILS.HAS_THREE_PHASE_POWER, details.getHasThreePhasePower())
          .set(PROPERTY_INDUSTRIAL_DETAILS.HAS_CRANE, details.getHasCrane())
          .set(PROPERTY_INDUSTRIAL_DETAILS.CRANE_CAPACITY_TONS, details.getCraneCapacityTons())
          .set(
              PROPERTY_INDUSTRIAL_DETAILS.HAS_HAZMAT_CERTIFICATION,
              details.getHasHazmatCertification())
          .set(
              PROPERTY_INDUSTRIAL_DETAILS.HAS_VENTILATION_SYSTEM, details.getHasVentilationSystem())
          .set(PROPERTY_INDUSTRIAL_DETAILS.HAS_CLIMATE_CONTROL, details.getHasClimateControl())
          .set(PROPERTY_INDUSTRIAL_DETAILS.YARD_AREA_VALUE, details.getYardAreaValue())
          .set(PROPERTY_INDUSTRIAL_DETAILS.YARD_AREA_UNIT, details.getYardAreaUnit())
          .set(PROPERTY_INDUSTRIAL_DETAILS.ZONING_CLASSIFICATION, details.getZoningClassification())
          .set(PROPERTY_INDUSTRIAL_DETAILS.CREATED_AT, now)
          .set(PROPERTY_INDUSTRIAL_DETAILS.UPDATED_AT, now)
          .set(PROPERTY_INDUSTRIAL_DETAILS.CREATED_BY, details.getCreatedBy())
          .set(PROPERTY_INDUSTRIAL_DETAILS.UPDATED_BY, details.getUpdatedBy())
          .execute();
      details.setId(newId);
      details.setCreatedAt(now.toInstant(UTC));
      details.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(PROPERTY_INDUSTRIAL_DETAILS)
          .set(PROPERTY_INDUSTRIAL_DETAILS.CLEAR_HEIGHT_M, details.getClearHeightM())
          .set(PROPERTY_INDUSTRIAL_DETAILS.LOADING_DOCKS, details.getLoadingDocks())
          .set(PROPERTY_INDUSTRIAL_DETAILS.DRIVE_IN_DOORS, details.getDriveInDoors())
          .set(
              PROPERTY_INDUSTRIAL_DETAILS.FLOOR_LOAD_CAPACITY_KG_SQM,
              details.getFloorLoadCapacityKgSqm())
          .set(PROPERTY_INDUSTRIAL_DETAILS.POWER_CAPACITY_KVA, details.getPowerCapacityKva())
          .set(PROPERTY_INDUSTRIAL_DETAILS.HAS_THREE_PHASE_POWER, details.getHasThreePhasePower())
          .set(PROPERTY_INDUSTRIAL_DETAILS.HAS_CRANE, details.getHasCrane())
          .set(PROPERTY_INDUSTRIAL_DETAILS.CRANE_CAPACITY_TONS, details.getCraneCapacityTons())
          .set(
              PROPERTY_INDUSTRIAL_DETAILS.HAS_HAZMAT_CERTIFICATION,
              details.getHasHazmatCertification())
          .set(
              PROPERTY_INDUSTRIAL_DETAILS.HAS_VENTILATION_SYSTEM, details.getHasVentilationSystem())
          .set(PROPERTY_INDUSTRIAL_DETAILS.HAS_CLIMATE_CONTROL, details.getHasClimateControl())
          .set(PROPERTY_INDUSTRIAL_DETAILS.YARD_AREA_VALUE, details.getYardAreaValue())
          .set(PROPERTY_INDUSTRIAL_DETAILS.YARD_AREA_UNIT, details.getYardAreaUnit())
          .set(PROPERTY_INDUSTRIAL_DETAILS.ZONING_CLASSIFICATION, details.getZoningClassification())
          .set(PROPERTY_INDUSTRIAL_DETAILS.UPDATED_AT, now)
          .set(PROPERTY_INDUSTRIAL_DETAILS.UPDATED_BY, details.getUpdatedBy())
          .where(
              PROPERTY_INDUSTRIAL_DETAILS
                  .ID
                  .eq(details.getId())
                  .and(PROPERTY_INDUSTRIAL_DETAILS.TEAM_ID.eq(details.getTeamId())))
          .execute();
      details.setUpdatedAt(now.toInstant(UTC));
    }
    return details;
  }

  public void deleteByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    dsl.deleteFrom(PROPERTY_INDUSTRIAL_DETAILS)
        .where(
            PROPERTY_INDUSTRIAL_DETAILS
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_INDUSTRIAL_DETAILS.TEAM_ID.eq(teamId)))
        .execute();
  }

  private PropertyIndustrialDetails toDomain(
      com.buurman.jooq.generated.tables.records.PropertyIndustrialDetailsRecord record) {
    PropertyIndustrialDetails d = new PropertyIndustrialDetails();
    d.setId(record.getId());
    d.setPropertyId(record.getPropertyId());
    d.setTeamId(record.getTeamId());
    d.setClearHeightM(record.getClearHeightM());
    d.setLoadingDocks(record.getLoadingDocks());
    d.setDriveInDoors(record.getDriveInDoors());
    d.setFloorLoadCapacityKgSqm(record.getFloorLoadCapacityKgSqm());
    d.setPowerCapacityKva(record.getPowerCapacityKva());
    d.setHasThreePhasePower(record.getHasThreePhasePower());
    d.setHasCrane(record.getHasCrane());
    d.setCraneCapacityTons(record.getCraneCapacityTons());
    d.setHasHazmatCertification(record.getHasHazmatCertification());
    d.setHasVentilationSystem(record.getHasVentilationSystem());
    d.setHasClimateControl(record.getHasClimateControl());
    d.setYardAreaValue(record.getYardAreaValue());
    d.setYardAreaUnit(record.getYardAreaUnit());
    d.setZoningClassification(record.getZoningClassification());
    d.setCreatedAt(record.getCreatedAt() == null ? null : record.getCreatedAt().toInstant(UTC));
    d.setUpdatedAt(record.getUpdatedAt() == null ? null : record.getUpdatedAt().toInstant(UTC));
    d.setCreatedBy(record.getCreatedBy());
    d.setUpdatedBy(record.getUpdatedBy());
    return d;
  }
}
