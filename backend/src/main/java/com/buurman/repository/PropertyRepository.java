package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.lower;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Property;
import com.buurman.dto.request.PageRequest;
import com.buurman.exception.NotFoundException;
import com.buurman.mapper.PropertyRecordMapper;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PropertyRepository {

  private final DSLContext dsl;
  private final PropertyRecordMapper mapper;
  private final Clock clock;

  public Optional<Property> findByIdentifierAndTeamId(String identifier, UUID teamId) {
    return dsl.selectFrom(PROPERTIES)
        .where(
            PROPERTIES
                .IDENTIFIER
                .eq(identifier)
                .and(PROPERTIES.TEAM_ID.eq(teamId))
                .and(PROPERTIES.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public Optional<Property> findByIdAndTeamId(UUID id, UUID teamId) {
    return dsl.selectFrom(PROPERTIES)
        .where(
            PROPERTIES
                .ID
                .eq(id)
                .and(PROPERTIES.TEAM_ID.eq(teamId))
                .and(PROPERTIES.DELETED_AT.isNull()))
        .fetchOptional()
        .map(mapper::toDomain);
  }

  public Property getByIdentifierAndTeamId(String identifier, UUID teamId) {
    return findByIdentifierAndTeamId(identifier, teamId)
        .orElseThrow(() -> new NotFoundException("Property not found"));
  }

  public Property getByIdAndTeamId(UUID id, UUID teamId) {
    return findByIdAndTeamId(id, teamId)
        .orElseThrow(() -> new NotFoundException("Property not found"));
  }

  public List<Property> findAllByTeamId(UUID teamId) {
    return List.copyOf(
        dsl.selectFrom(PROPERTIES)
            .where(PROPERTIES.TEAM_ID.eq(teamId).and(PROPERTIES.DELETED_AT.isNull()))
            .orderBy(PROPERTIES.CREATED_AT.desc())
            .fetch()
            .map(mapper::toDomain));
  }

  public List<Property> findByTeamIdAndStatus(UUID teamId, Property.PropertyStatus status) {
    return List.copyOf(
        dsl.selectFrom(PROPERTIES)
            .where(
                PROPERTIES
                    .TEAM_ID
                    .eq(teamId)
                    .and(PROPERTIES.STATUS.eq(status.name()))
                    .and(PROPERTIES.DELETED_AT.isNull()))
            .orderBy(PROPERTIES.CREATED_AT.desc())
            .fetch()
            .map(mapper::toDomain));
  }

  public Property save(Property property) {
    LocalDateTime now = LocalDateTime.now(clock);

    if (property.getId() == null) {
      // INSERT
      UUID newId = UUID.randomUUID();
      LocalDateTime createdAt =
          property.getCreatedAt() != null
              ? LocalDateTime.ofInstant(property.getCreatedAt(), UTC)
              : now;
      LocalDateTime updatedAt =
          property.getUpdatedAt() != null
              ? LocalDateTime.ofInstant(property.getUpdatedAt(), UTC)
              : now;

      dsl.insertInto(PROPERTIES)
          .set(PROPERTIES.ID, newId)
          .set(PROPERTIES.IDENTIFIER, property.getIdentifier())
          .set(PROPERTIES.TEAM_ID, property.getTeamId())
          .set(PROPERTIES.STREET, property.getStreet())
          .set(PROPERTIES.CITY, property.getCity())
          .set(PROPERTIES.POSTAL_CODE, property.getPostalCode())
          .set(PROPERTIES.COUNTRY, property.getCountry())
          .set(PROPERTIES.LATITUDE, property.getLatitude().orElse(null))
          .set(PROPERTIES.LONGITUDE, property.getLongitude().orElse(null))
          .set(PROPERTIES.GEOCODE_ACCURACY, property.getGeocodeAccuracy().orElse(null))
          .set(PROPERTIES.AREA_VALUE, property.getAreaValue().orElse(null))
          .set(PROPERTIES.AREA_UNIT, property.getAreaUnit().orElse(null))
          .set(PROPERTIES.PROPERTY_CATEGORY, property.getPropertyCategory().name())
          .set(PROPERTIES.PROPERTY_TYPE, property.getPropertyType().name())
          .set(PROPERTIES.STATUS, property.getStatus().name())
          // Construction & Structure
          .set(PROPERTIES.YEAR_BUILT, property.getYearBuilt().orElse(null))
          .set(PROPERTIES.YEAR_LAST_RENOVATED, property.getYearLastRenovated().orElse(null))
          .set(PROPERTIES.CONSTRUCTION_TYPE, property.getConstructionType().orElse(null))
          .set(PROPERTIES.FOUNDATION_TYPE, property.getFoundationType().orElse(null))
          .set(PROPERTIES.ROOF_TYPE, property.getRoofType().orElse(null))
          .set(PROPERTIES.WALL_CONSTRUCTION, property.getWallConstruction().orElse(null))
          .set(PROPERTIES.FLOORING_TYPE, property.getFlooringType().orElse(null))
          .set(PROPERTIES.WINDOW_TYPE, property.getWindowType().orElse(null))
          .set(PROPERTIES.NUMBER_OF_FLOORS, property.getNumberOfFloors().orElse(null))
          .set(PROPERTIES.STRUCTURAL_NOTES, property.getStructuralNotes().orElse(null))
          // Energy & Climate
          .set(
              PROPERTIES.ENERGY_EFFICIENCY_RATING,
              property.getEnergyEfficiencyRating().orElse(null))
          .set(
              PROPERTIES.ENERGY_CERTIFICATE_EXPIRY_DATE,
              property.getEnergyCertificateExpiryDate().orElse(null))
          .set(PROPERTIES.HEATING_TYPE, property.getHeatingType().orElse(null))
          .set(PROPERTIES.COOLING_TYPE, property.getCoolingType().orElse(null))
          .set(PROPERTIES.HOT_WATER_SYSTEM, property.getHotWaterSystem().orElse(null))
          .set(PROPERTIES.INSULATION_NOTES, property.getInsulationNotes().orElse(null))
          // Utilities & Connections
          .set(
              PROPERTIES.ELECTRICITY_CONNECTION_TYPE,
              property.getElectricityConnectionType().orElse(null))
          .set(
              PROPERTIES.ELECTRICITY_CAPACITY_AMPS,
              property.getElectricityCapacityAmps().orElse(null))
          .set(PROPERTIES.WATER_CONNECTION_TYPE, property.getWaterConnectionType().orElse(null))
          .set(PROPERTIES.HAS_GAS_CONNECTION, property.getHasGasConnection().orElse(null))
          .set(PROPERTIES.SEWAGE_TYPE, property.getSewageType().orElse(null))
          .set(
              PROPERTIES.INTERNET_CONNECTION_TYPE,
              property.getInternetConnectionType().orElse(null))
          .set(PROPERTIES.INTERNET_MAX_SPEED_MBPS, property.getInternetMaxSpeedMbps().orElse(null))
          .set(PROPERTIES.INTERNET_STATUS, property.getInternetStatus().orElse(null))
          // Parking
          .set(PROPERTIES.PARKING_SPACES, property.getParkingSpaces().orElse(null))
          .set(PROPERTIES.PARKING_TYPE, property.getParkingType().orElse(null))
          // Safety & Security
          .set(PROPERTIES.HAS_SMOKE_DETECTORS, property.getHasSmokeDetectors().orElse(null))
          .set(PROPERTIES.HAS_CO_DETECTORS, property.getHasCoDetectors().orElse(null))
          .set(PROPERTIES.HAS_FIRE_EXTINGUISHER, property.getHasFireExtinguisher().orElse(null))
          .set(PROPERTIES.HAS_SPRINKLER_SYSTEM, property.getHasSprinklerSystem().orElse(null))
          .set(PROPERTIES.HAS_ALARM_SYSTEM, property.getHasAlarmSystem().orElse(null))
          .set(PROPERTIES.HAS_SECURITY_CAMERAS, property.getHasSecurityCameras().orElse(null))
          .set(PROPERTIES.HAS_SECURE_ENTRY, property.getHasSecureEntry().orElse(null))
          .set(PROPERTIES.SAFETY_NOTES, property.getSafetyNotes().orElse(null))
          // Accessibility
          .set(
              PROPERTIES.IS_WHEELCHAIR_ACCESSIBLE,
              property.getIsWheelchairAccessible().orElse(null))
          .set(PROPERTIES.HAS_ELEVATOR, property.getHasElevator().orElse(null))
          .set(PROPERTIES.HAS_STEP_FREE_ENTRANCE, property.getHasStepFreeEntrance().orElse(null))
          .set(PROPERTIES.HAS_ADAPTED_BATHROOM, property.getHasAdaptedBathroom().orElse(null))
          .set(PROPERTIES.ACCESSIBILITY_NOTES, property.getAccessibilityNotes().orElse(null))
          // Audit
          .set(PROPERTIES.CREATED_AT, createdAt)
          .set(PROPERTIES.UPDATED_AT, updatedAt)
          .set(PROPERTIES.CREATED_BY, property.getCreatedBy())
          .set(PROPERTIES.UPDATED_BY, property.getUpdatedBy())
          .execute();

      property.setId(newId);
      property.setCreatedAt(createdAt.toInstant(UTC));
      property.setUpdatedAt(updatedAt.toInstant(UTC));
    } else {
      // UPDATE
      LocalDateTime updatedAt =
          property.getUpdatedAt() != null
              ? LocalDateTime.ofInstant(property.getUpdatedAt(), UTC)
              : now;

      dsl.update(PROPERTIES)
          .set(PROPERTIES.STREET, property.getStreet())
          .set(PROPERTIES.CITY, property.getCity())
          .set(PROPERTIES.POSTAL_CODE, property.getPostalCode())
          .set(PROPERTIES.COUNTRY, property.getCountry())
          .set(PROPERTIES.LATITUDE, property.getLatitude().orElse(null))
          .set(PROPERTIES.LONGITUDE, property.getLongitude().orElse(null))
          .set(PROPERTIES.GEOCODE_ACCURACY, property.getGeocodeAccuracy().orElse(null))
          .set(PROPERTIES.AREA_VALUE, property.getAreaValue().orElse(null))
          .set(PROPERTIES.AREA_UNIT, property.getAreaUnit().orElse(null))
          .set(PROPERTIES.PROPERTY_TYPE, property.getPropertyType().name())
          .set(PROPERTIES.STATUS, property.getStatus().name())
          // Note: property_category is NOT updated (immutable)
          // Construction & Structure
          .set(PROPERTIES.YEAR_BUILT, property.getYearBuilt().orElse(null))
          .set(PROPERTIES.YEAR_LAST_RENOVATED, property.getYearLastRenovated().orElse(null))
          .set(PROPERTIES.CONSTRUCTION_TYPE, property.getConstructionType().orElse(null))
          .set(PROPERTIES.FOUNDATION_TYPE, property.getFoundationType().orElse(null))
          .set(PROPERTIES.ROOF_TYPE, property.getRoofType().orElse(null))
          .set(PROPERTIES.WALL_CONSTRUCTION, property.getWallConstruction().orElse(null))
          .set(PROPERTIES.FLOORING_TYPE, property.getFlooringType().orElse(null))
          .set(PROPERTIES.WINDOW_TYPE, property.getWindowType().orElse(null))
          .set(PROPERTIES.NUMBER_OF_FLOORS, property.getNumberOfFloors().orElse(null))
          .set(PROPERTIES.STRUCTURAL_NOTES, property.getStructuralNotes().orElse(null))
          // Energy & Climate
          .set(
              PROPERTIES.ENERGY_EFFICIENCY_RATING,
              property.getEnergyEfficiencyRating().orElse(null))
          .set(
              PROPERTIES.ENERGY_CERTIFICATE_EXPIRY_DATE,
              property.getEnergyCertificateExpiryDate().orElse(null))
          .set(PROPERTIES.HEATING_TYPE, property.getHeatingType().orElse(null))
          .set(PROPERTIES.COOLING_TYPE, property.getCoolingType().orElse(null))
          .set(PROPERTIES.HOT_WATER_SYSTEM, property.getHotWaterSystem().orElse(null))
          .set(PROPERTIES.INSULATION_NOTES, property.getInsulationNotes().orElse(null))
          // Utilities & Connections
          .set(
              PROPERTIES.ELECTRICITY_CONNECTION_TYPE,
              property.getElectricityConnectionType().orElse(null))
          .set(
              PROPERTIES.ELECTRICITY_CAPACITY_AMPS,
              property.getElectricityCapacityAmps().orElse(null))
          .set(PROPERTIES.WATER_CONNECTION_TYPE, property.getWaterConnectionType().orElse(null))
          .set(PROPERTIES.HAS_GAS_CONNECTION, property.getHasGasConnection().orElse(null))
          .set(PROPERTIES.SEWAGE_TYPE, property.getSewageType().orElse(null))
          .set(
              PROPERTIES.INTERNET_CONNECTION_TYPE,
              property.getInternetConnectionType().orElse(null))
          .set(PROPERTIES.INTERNET_MAX_SPEED_MBPS, property.getInternetMaxSpeedMbps().orElse(null))
          .set(PROPERTIES.INTERNET_STATUS, property.getInternetStatus().orElse(null))
          // Parking
          .set(PROPERTIES.PARKING_SPACES, property.getParkingSpaces().orElse(null))
          .set(PROPERTIES.PARKING_TYPE, property.getParkingType().orElse(null))
          // Safety & Security
          .set(PROPERTIES.HAS_SMOKE_DETECTORS, property.getHasSmokeDetectors().orElse(null))
          .set(PROPERTIES.HAS_CO_DETECTORS, property.getHasCoDetectors().orElse(null))
          .set(PROPERTIES.HAS_FIRE_EXTINGUISHER, property.getHasFireExtinguisher().orElse(null))
          .set(PROPERTIES.HAS_SPRINKLER_SYSTEM, property.getHasSprinklerSystem().orElse(null))
          .set(PROPERTIES.HAS_ALARM_SYSTEM, property.getHasAlarmSystem().orElse(null))
          .set(PROPERTIES.HAS_SECURITY_CAMERAS, property.getHasSecurityCameras().orElse(null))
          .set(PROPERTIES.HAS_SECURE_ENTRY, property.getHasSecureEntry().orElse(null))
          .set(PROPERTIES.SAFETY_NOTES, property.getSafetyNotes().orElse(null))
          // Accessibility
          .set(
              PROPERTIES.IS_WHEELCHAIR_ACCESSIBLE,
              property.getIsWheelchairAccessible().orElse(null))
          .set(PROPERTIES.HAS_ELEVATOR, property.getHasElevator().orElse(null))
          .set(PROPERTIES.HAS_STEP_FREE_ENTRANCE, property.getHasStepFreeEntrance().orElse(null))
          .set(PROPERTIES.HAS_ADAPTED_BATHROOM, property.getHasAdaptedBathroom().orElse(null))
          .set(PROPERTIES.ACCESSIBILITY_NOTES, property.getAccessibilityNotes().orElse(null))
          // Audit
          .set(PROPERTIES.UPDATED_AT, updatedAt)
          .set(PROPERTIES.UPDATED_BY, property.getUpdatedBy())
          .where(
              PROPERTIES.ID.eq(property.getId()).and(PROPERTIES.TEAM_ID.eq(property.getTeamId())))
          .execute();

      property.setUpdatedAt(updatedAt.toInstant(UTC));
    }

    return property;
  }

  public PaginatedResult<Property> findAllByTeamIdPaginated(
      UUID teamId,
      @Nullable String status,
      @Nullable String category,
      @Nullable String query,
      PageRequest pageRequest) {
    Condition condition = PROPERTIES.TEAM_ID.eq(teamId).and(PROPERTIES.DELETED_AT.isNull());
    if (status != null && !status.isEmpty()) {
      condition = condition.and(PROPERTIES.STATUS.eq(status));
    }
    if (category != null && !category.isEmpty()) {
      condition = condition.and(PROPERTIES.PROPERTY_CATEGORY.eq(category));
    }
    if (query != null && !query.isBlank()) {
      String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
      condition =
          condition.and(
              lower(PROPERTIES.STREET)
                  .like(like)
                  .or(lower(PROPERTIES.CITY).like(like))
                  .or(lower(PROPERTIES.POSTAL_CODE).like(like))
                  .or(lower(PROPERTIES.IDENTIFIER).like(like))
                  .or(lower(PROPERTIES.PROPERTY_TYPE).like(like)));
    }
    Map<String, Field<?>> sortableFields =
        Map.of(
            "createdAt", PROPERTIES.CREATED_AT,
            "updatedAt", PROPERTIES.UPDATED_AT,
            "street", PROPERTIES.STREET,
            "city", PROPERTIES.CITY,
            "status", PROPERTIES.STATUS,
            "propertyType", PROPERTIES.PROPERTY_TYPE,
            "propertyCategory", PROPERTIES.PROPERTY_CATEGORY);
    return PaginationHelper.paginate(
        dsl,
        PROPERTIES,
        condition,
        sortableFields,
        PROPERTIES.CREATED_AT,
        pageRequest,
        mapper::toDomain);
  }

  public List<Property> findByIdsAndTeamId(Collection<UUID> ids, UUID teamId) {
    if (ids == null || ids.isEmpty()) {
      return List.of();
    }
    return List.copyOf(
        dsl.selectFrom(PROPERTIES)
            .where(
                PROPERTIES
                    .ID
                    .in(ids)
                    .and(PROPERTIES.TEAM_ID.eq(teamId))
                    .and(PROPERTIES.DELETED_AT.isNull()))
            .fetch()
            .map(mapper::toDomain));
  }

  public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
    LocalDateTime now = LocalDateTime.now(clock);
    dsl.update(PROPERTIES)
        .set(PROPERTIES.DELETED_AT, now)
        .where(PROPERTIES.ID.eq(id).and(PROPERTIES.TEAM_ID.eq(teamId)))
        .execute();
  }
}
