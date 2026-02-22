package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTY_AGRICULTURAL_DETAILS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PropertyAgriculturalDetails;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PropertyAgriculturalDetailsRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public Optional<PropertyAgriculturalDetails> findByPropertyIdAndTeamId(
      UUID propertyId, UUID teamId) {
    return dsl.selectFrom(PROPERTY_AGRICULTURAL_DETAILS)
        .where(
            PROPERTY_AGRICULTURAL_DETAILS
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_AGRICULTURAL_DETAILS.TEAM_ID.eq(teamId)))
        .fetchOptional()
        .map(this::toDomain);
  }

  public PropertyAgriculturalDetails save(PropertyAgriculturalDetails details) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (details.getId() == null) {
      UUID newId = UUID.randomUUID();
      dsl.insertInto(PROPERTY_AGRICULTURAL_DETAILS)
          .set(PROPERTY_AGRICULTURAL_DETAILS.ID, newId)
          .set(PROPERTY_AGRICULTURAL_DETAILS.PROPERTY_ID, details.getPropertyId())
          .set(PROPERTY_AGRICULTURAL_DETAILS.TEAM_ID, details.getTeamId())
          .set(PROPERTY_AGRICULTURAL_DETAILS.TOTAL_LAND_AREA_VALUE, details.getTotalLandAreaValue())
          .set(PROPERTY_AGRICULTURAL_DETAILS.TOTAL_LAND_AREA_UNIT, details.getTotalLandAreaUnit())
          .set(PROPERTY_AGRICULTURAL_DETAILS.ARABLE_AREA_VALUE, details.getArableAreaValue())
          .set(PROPERTY_AGRICULTURAL_DETAILS.ARABLE_AREA_UNIT, details.getArableAreaUnit())
          .set(PROPERTY_AGRICULTURAL_DETAILS.SOIL_TYPE, details.getSoilType())
          .set(PROPERTY_AGRICULTURAL_DETAILS.HAS_WATER_RIGHTS, details.getHasWaterRights())
          .set(PROPERTY_AGRICULTURAL_DETAILS.WATER_SOURCE, details.getWaterSource())
          .set(PROPERTY_AGRICULTURAL_DETAILS.IRRIGATION_TYPE, details.getIrrigationType())
          .set(PROPERTY_AGRICULTURAL_DETAILS.FENCING_TYPE, details.getFencingType())
          .set(PROPERTY_AGRICULTURAL_DETAILS.HAS_OUTBUILDINGS, details.getHasOutbuildings())
          .set(PROPERTY_AGRICULTURAL_DETAILS.OUTBUILDING_DETAILS, details.getOutbuildingDetails())
          .set(PROPERTY_AGRICULTURAL_DETAILS.CURRENT_USE, details.getCurrentUse())
          .set(
              PROPERTY_AGRICULTURAL_DETAILS.ZONING_CLASSIFICATION,
              details.getZoningClassification())
          .set(PROPERTY_AGRICULTURAL_DETAILS.CREATED_AT, now)
          .set(PROPERTY_AGRICULTURAL_DETAILS.UPDATED_AT, now)
          .set(PROPERTY_AGRICULTURAL_DETAILS.CREATED_BY, details.getCreatedBy())
          .set(PROPERTY_AGRICULTURAL_DETAILS.UPDATED_BY, details.getUpdatedBy())
          .execute();
      details.setId(newId);
      details.setCreatedAt(now.toInstant(UTC));
      details.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(PROPERTY_AGRICULTURAL_DETAILS)
          .set(PROPERTY_AGRICULTURAL_DETAILS.TOTAL_LAND_AREA_VALUE, details.getTotalLandAreaValue())
          .set(PROPERTY_AGRICULTURAL_DETAILS.TOTAL_LAND_AREA_UNIT, details.getTotalLandAreaUnit())
          .set(PROPERTY_AGRICULTURAL_DETAILS.ARABLE_AREA_VALUE, details.getArableAreaValue())
          .set(PROPERTY_AGRICULTURAL_DETAILS.ARABLE_AREA_UNIT, details.getArableAreaUnit())
          .set(PROPERTY_AGRICULTURAL_DETAILS.SOIL_TYPE, details.getSoilType())
          .set(PROPERTY_AGRICULTURAL_DETAILS.HAS_WATER_RIGHTS, details.getHasWaterRights())
          .set(PROPERTY_AGRICULTURAL_DETAILS.WATER_SOURCE, details.getWaterSource())
          .set(PROPERTY_AGRICULTURAL_DETAILS.IRRIGATION_TYPE, details.getIrrigationType())
          .set(PROPERTY_AGRICULTURAL_DETAILS.FENCING_TYPE, details.getFencingType())
          .set(PROPERTY_AGRICULTURAL_DETAILS.HAS_OUTBUILDINGS, details.getHasOutbuildings())
          .set(PROPERTY_AGRICULTURAL_DETAILS.OUTBUILDING_DETAILS, details.getOutbuildingDetails())
          .set(PROPERTY_AGRICULTURAL_DETAILS.CURRENT_USE, details.getCurrentUse())
          .set(
              PROPERTY_AGRICULTURAL_DETAILS.ZONING_CLASSIFICATION,
              details.getZoningClassification())
          .set(PROPERTY_AGRICULTURAL_DETAILS.UPDATED_AT, now)
          .set(PROPERTY_AGRICULTURAL_DETAILS.UPDATED_BY, details.getUpdatedBy())
          .where(
              PROPERTY_AGRICULTURAL_DETAILS
                  .ID
                  .eq(details.getId())
                  .and(PROPERTY_AGRICULTURAL_DETAILS.TEAM_ID.eq(details.getTeamId())))
          .execute();
      details.setUpdatedAt(now.toInstant(UTC));
    }
    return details;
  }

  public void deleteByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    dsl.deleteFrom(PROPERTY_AGRICULTURAL_DETAILS)
        .where(
            PROPERTY_AGRICULTURAL_DETAILS
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_AGRICULTURAL_DETAILS.TEAM_ID.eq(teamId)))
        .execute();
  }

  private PropertyAgriculturalDetails toDomain(
      com.buurman.jooq.generated.tables.records.PropertyAgriculturalDetailsRecord record) {
    PropertyAgriculturalDetails d = new PropertyAgriculturalDetails();
    d.setId(record.getId());
    d.setPropertyId(record.getPropertyId());
    d.setTeamId(record.getTeamId());
    d.setTotalLandAreaValue(record.getTotalLandAreaValue());
    d.setTotalLandAreaUnit(record.getTotalLandAreaUnit());
    d.setArableAreaValue(record.getArableAreaValue());
    d.setArableAreaUnit(record.getArableAreaUnit());
    d.setSoilType(record.getSoilType());
    d.setHasWaterRights(record.getHasWaterRights());
    d.setWaterSource(record.getWaterSource());
    d.setIrrigationType(record.getIrrigationType());
    d.setFencingType(record.getFencingType());
    d.setHasOutbuildings(record.getHasOutbuildings());
    d.setOutbuildingDetails(record.getOutbuildingDetails());
    d.setCurrentUse(record.getCurrentUse());
    d.setZoningClassification(record.getZoningClassification());
    d.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    d.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    d.setCreatedBy(record.getCreatedBy());
    d.setUpdatedBy(record.getUpdatedBy());
    return d;
  }
}
