package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTY_COMMERCIAL_DETAILS;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import com.buurman.domain.PropertyCommercialDetails;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PropertyCommercialDetailsRepository {

  private final DSLContext dsl;
  private final Clock clock;

  public Optional<PropertyCommercialDetails> findByPropertyIdAndTeamId(
      UUID propertyId, UUID teamId) {
    return dsl.selectFrom(PROPERTY_COMMERCIAL_DETAILS)
        .where(
            PROPERTY_COMMERCIAL_DETAILS
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_COMMERCIAL_DETAILS.TEAM_ID.eq(teamId)))
        .fetchOptional()
        .map(this::toDomain);
  }

  public PropertyCommercialDetails save(PropertyCommercialDetails details) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (details.getId() == null) {
      UUID newId = UUID.randomUUID();
      dsl.insertInto(PROPERTY_COMMERCIAL_DETAILS)
          .set(PROPERTY_COMMERCIAL_DETAILS.ID, newId)
          .set(PROPERTY_COMMERCIAL_DETAILS.PROPERTY_ID, details.getPropertyId())
          .set(PROPERTY_COMMERCIAL_DETAILS.TEAM_ID, details.getTeamId())
          .set(PROPERTY_COMMERCIAL_DETAILS.USABLE_AREA_VALUE, details.getUsableAreaValue())
          .set(PROPERTY_COMMERCIAL_DETAILS.USABLE_AREA_UNIT, details.getUsableAreaUnit())
          .set(PROPERTY_COMMERCIAL_DETAILS.COMMON_AREA_VALUE, details.getCommonAreaValue())
          .set(PROPERTY_COMMERCIAL_DETAILS.COMMON_AREA_UNIT, details.getCommonAreaUnit())
          .set(PROPERTY_COMMERCIAL_DETAILS.FLOOR_LEVEL, details.getFloorLevel())
          .set(PROPERTY_COMMERCIAL_DETAILS.CEILING_HEIGHT_M, details.getCeilingHeightM())
          .set(PROPERTY_COMMERCIAL_DETAILS.HAS_STOREFRONT, details.getHasStorefront())
          .set(PROPERTY_COMMERCIAL_DETAILS.HAS_SIGNAGE_RIGHTS, details.getHasSignageRights())
          .set(PROPERTY_COMMERCIAL_DETAILS.ZONING_CLASSIFICATION, details.getZoningClassification())
          .set(PROPERTY_COMMERCIAL_DETAILS.MAX_OCCUPANCY, details.getMaxOccupancy())
          .set(PROPERTY_COMMERCIAL_DETAILS.RESTROOM_COUNT, details.getRestroomCount())
          .set(PROPERTY_COMMERCIAL_DETAILS.HAS_KITCHEN_FACILITY, details.getHasKitchenFacility())
          .set(
              PROPERTY_COMMERCIAL_DETAILS.ACCESSIBILITY_COMPLIANT,
              details.getAccessibilityCompliant())
          .set(PROPERTY_COMMERCIAL_DETAILS.CREATED_AT, now)
          .set(PROPERTY_COMMERCIAL_DETAILS.UPDATED_AT, now)
          .set(PROPERTY_COMMERCIAL_DETAILS.CREATED_BY, details.getCreatedBy())
          .set(PROPERTY_COMMERCIAL_DETAILS.UPDATED_BY, details.getUpdatedBy())
          .execute();
      details.setId(newId);
      details.setCreatedAt(now.toInstant(UTC));
      details.setUpdatedAt(now.toInstant(UTC));
    } else {
      dsl.update(PROPERTY_COMMERCIAL_DETAILS)
          .set(PROPERTY_COMMERCIAL_DETAILS.USABLE_AREA_VALUE, details.getUsableAreaValue())
          .set(PROPERTY_COMMERCIAL_DETAILS.USABLE_AREA_UNIT, details.getUsableAreaUnit())
          .set(PROPERTY_COMMERCIAL_DETAILS.COMMON_AREA_VALUE, details.getCommonAreaValue())
          .set(PROPERTY_COMMERCIAL_DETAILS.COMMON_AREA_UNIT, details.getCommonAreaUnit())
          .set(PROPERTY_COMMERCIAL_DETAILS.FLOOR_LEVEL, details.getFloorLevel())
          .set(PROPERTY_COMMERCIAL_DETAILS.CEILING_HEIGHT_M, details.getCeilingHeightM())
          .set(PROPERTY_COMMERCIAL_DETAILS.HAS_STOREFRONT, details.getHasStorefront())
          .set(PROPERTY_COMMERCIAL_DETAILS.HAS_SIGNAGE_RIGHTS, details.getHasSignageRights())
          .set(PROPERTY_COMMERCIAL_DETAILS.ZONING_CLASSIFICATION, details.getZoningClassification())
          .set(PROPERTY_COMMERCIAL_DETAILS.MAX_OCCUPANCY, details.getMaxOccupancy())
          .set(PROPERTY_COMMERCIAL_DETAILS.RESTROOM_COUNT, details.getRestroomCount())
          .set(PROPERTY_COMMERCIAL_DETAILS.HAS_KITCHEN_FACILITY, details.getHasKitchenFacility())
          .set(
              PROPERTY_COMMERCIAL_DETAILS.ACCESSIBILITY_COMPLIANT,
              details.getAccessibilityCompliant())
          .set(PROPERTY_COMMERCIAL_DETAILS.UPDATED_AT, now)
          .set(PROPERTY_COMMERCIAL_DETAILS.UPDATED_BY, details.getUpdatedBy())
          .where(
              PROPERTY_COMMERCIAL_DETAILS
                  .ID
                  .eq(details.getId())
                  .and(PROPERTY_COMMERCIAL_DETAILS.TEAM_ID.eq(details.getTeamId())))
          .execute();
      details.setUpdatedAt(now.toInstant(UTC));
    }
    return details;
  }

  public void deleteByPropertyIdAndTeamId(UUID propertyId, UUID teamId) {
    dsl.deleteFrom(PROPERTY_COMMERCIAL_DETAILS)
        .where(
            PROPERTY_COMMERCIAL_DETAILS
                .PROPERTY_ID
                .eq(propertyId)
                .and(PROPERTY_COMMERCIAL_DETAILS.TEAM_ID.eq(teamId)))
        .execute();
  }

  private PropertyCommercialDetails toDomain(
      com.buurman.jooq.generated.tables.records.PropertyCommercialDetailsRecord record) {
    PropertyCommercialDetails d = new PropertyCommercialDetails();
    d.setId(record.getId());
    d.setPropertyId(record.getPropertyId());
    d.setTeamId(record.getTeamId());
    d.setUsableAreaValue(record.getUsableAreaValue());
    d.setUsableAreaUnit(record.getUsableAreaUnit());
    d.setCommonAreaValue(record.getCommonAreaValue());
    d.setCommonAreaUnit(record.getCommonAreaUnit());
    d.setFloorLevel(record.getFloorLevel());
    d.setCeilingHeightM(record.getCeilingHeightM());
    d.setHasStorefront(record.getHasStorefront());
    d.setHasSignageRights(record.getHasSignageRights());
    d.setZoningClassification(record.getZoningClassification());
    d.setMaxOccupancy(record.getMaxOccupancy());
    d.setRestroomCount(record.getRestroomCount());
    d.setHasKitchenFacility(record.getHasKitchenFacility());
    d.setAccessibilityCompliant(record.getAccessibilityCompliant());
    d.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    d.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    d.setCreatedBy(record.getCreatedBy());
    d.setUpdatedBy(record.getUpdatedBy());
    return d;
  }
}
