package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static com.buurman.jooq.generated.Tables.UNITS;
import static java.time.ZoneOffset.UTC;
import static org.jooq.impl.DSL.lower;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record4;
import org.jooq.impl.DSL;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Repository;

import com.buurman.domain.Property;
import com.buurman.domain.Sid;
import com.buurman.domain.UnitStatus;
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

  public Optional<Property> findByIdentifierAndTeamId(Sid identifier, UUID teamId) {
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

  public Property getByIdentifierAndTeamId(Sid identifier, UUID teamId) {
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

  /**
   * "A property with status X" now means "a property with at least one non-deleted unit whose
   * status is X" — status moved from {@code properties} to {@code units} in V068, and a property
   * itself no longer carries a single status once it can hold several independently-let units.
   */
  public List<Property> findByTeamIdAndStatus(UUID teamId, UnitStatus status) {
    return List.copyOf(
        dsl.selectFrom(PROPERTIES)
            .where(
                PROPERTIES
                    .TEAM_ID
                    .eq(teamId)
                    .and(PROPERTIES.DELETED_AT.isNull())
                    .and(hasUnitWithStatus(teamId, status)))
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
          .set(PROPERTIES.IDENTIFIER, property.getIdentifier().orElseThrow())
          .set(PROPERTIES.TEAM_ID, property.getTeamId())
          .set(PROPERTIES.STREET, property.getStreet())
          .set(PROPERTIES.CITY, property.getCity())
          .set(PROPERTIES.POSTAL_CODE, property.getPostalCode())
          .set(PROPERTIES.COUNTRY_CODE, property.getCountryCode())
          .set(PROPERTIES.REGION_CODE, property.getRegionCode().orElse(null))
          .set(PROPERTIES.LATITUDE, property.getLatitude().orElse(null))
          .set(PROPERTIES.LONGITUDE, property.getLongitude().orElse(null))
          .set(PROPERTIES.GEOCODE_ACCURACY, property.getGeocodeAccuracy().orElse(null))
          .set(PROPERTIES.PROPERTY_CATEGORY, property.getPropertyCategory().name())
          .set(PROPERTIES.PROPERTY_TYPE, property.getPropertyType().name())
          .set(PROPERTIES.ALLOCATION_BASIS, property.getAllocationBasis().name())
          // Construction & Structure
          .set(PROPERTIES.YEAR_BUILT, property.getYearBuilt().orElse(null))
          .set(PROPERTIES.YEAR_LAST_RENOVATED, property.getYearLastRenovated().orElse(null))
          .set(PROPERTIES.CONSTRUCTION_TYPE, property.getConstructionType().orElse(null))
          .set(PROPERTIES.FOUNDATION_TYPE, property.getFoundationType().orElse(null))
          .set(PROPERTIES.ROOF_TYPE, property.getRoofType().orElse(null))
          .set(PROPERTIES.WALL_CONSTRUCTION, property.getWallConstruction().orElse(null))
          .set(PROPERTIES.NUMBER_OF_FLOORS, property.getNumberOfFloors().orElse(null))
          .set(PROPERTIES.STRUCTURAL_NOTES, property.getStructuralNotes().orElse(null))
          // Utilities & Connections
          .set(
              PROPERTIES.ELECTRICITY_CONNECTION_TYPE,
              property.getElectricityConnectionType().orElse(null))
          .set(
              PROPERTIES.ELECTRICITY_CAPACITY_VALUE,
              property.getElectricityCapacityValue().orElse(null))
          .set(
              PROPERTIES.ELECTRICITY_CAPACITY_UNIT,
              property.getElectricityCapacityUnit().orElse(null))
          .set(PROPERTIES.WATER_CONNECTION_TYPE, property.getWaterConnectionType().orElse(null))
          .set(PROPERTIES.HAS_GAS_CONNECTION, property.getHasGasConnection().orElse(null))
          .set(PROPERTIES.SEWAGE_TYPE, property.getSewageType().orElse(null))
          .set(
              PROPERTIES.INTERNET_CONNECTION_TYPE,
              property.getInternetConnectionType().orElse(null))
          .set(
              PROPERTIES.INTERNET_MAX_SPEED_VALUE, property.getInternetMaxSpeedValue().orElse(null))
          .set(PROPERTIES.INTERNET_MAX_SPEED_UNIT, property.getInternetMaxSpeedUnit().orElse(null))
          .set(PROPERTIES.INTERNET_STATUS, property.getInternetStatus().orElse(null))
          // Parking
          .set(PROPERTIES.PARKING_SPACES, property.getParkingSpaces().orElse(null))
          .set(PROPERTIES.PARKING_TYPE, property.getParkingType().orElse(null))
          // Safety & Security
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
          .set(PROPERTIES.COUNTRY_CODE, property.getCountryCode())
          .set(PROPERTIES.REGION_CODE, property.getRegionCode().orElse(null))
          .set(PROPERTIES.LATITUDE, property.getLatitude().orElse(null))
          .set(PROPERTIES.LONGITUDE, property.getLongitude().orElse(null))
          .set(PROPERTIES.GEOCODE_ACCURACY, property.getGeocodeAccuracy().orElse(null))
          .set(PROPERTIES.PROPERTY_TYPE, property.getPropertyType().name())
          .set(PROPERTIES.ALLOCATION_BASIS, property.getAllocationBasis().name())
          // Note: property_category is NOT updated (immutable)
          // Construction & Structure
          .set(PROPERTIES.YEAR_BUILT, property.getYearBuilt().orElse(null))
          .set(PROPERTIES.YEAR_LAST_RENOVATED, property.getYearLastRenovated().orElse(null))
          .set(PROPERTIES.CONSTRUCTION_TYPE, property.getConstructionType().orElse(null))
          .set(PROPERTIES.FOUNDATION_TYPE, property.getFoundationType().orElse(null))
          .set(PROPERTIES.ROOF_TYPE, property.getRoofType().orElse(null))
          .set(PROPERTIES.WALL_CONSTRUCTION, property.getWallConstruction().orElse(null))
          .set(PROPERTIES.NUMBER_OF_FLOORS, property.getNumberOfFloors().orElse(null))
          .set(PROPERTIES.STRUCTURAL_NOTES, property.getStructuralNotes().orElse(null))
          // Utilities & Connections
          .set(
              PROPERTIES.ELECTRICITY_CONNECTION_TYPE,
              property.getElectricityConnectionType().orElse(null))
          .set(
              PROPERTIES.ELECTRICITY_CAPACITY_VALUE,
              property.getElectricityCapacityValue().orElse(null))
          .set(
              PROPERTIES.ELECTRICITY_CAPACITY_UNIT,
              property.getElectricityCapacityUnit().orElse(null))
          .set(PROPERTIES.WATER_CONNECTION_TYPE, property.getWaterConnectionType().orElse(null))
          .set(PROPERTIES.HAS_GAS_CONNECTION, property.getHasGasConnection().orElse(null))
          .set(PROPERTIES.SEWAGE_TYPE, property.getSewageType().orElse(null))
          .set(
              PROPERTIES.INTERNET_CONNECTION_TYPE,
              property.getInternetConnectionType().orElse(null))
          .set(
              PROPERTIES.INTERNET_MAX_SPEED_VALUE, property.getInternetMaxSpeedValue().orElse(null))
          .set(PROPERTIES.INTERNET_MAX_SPEED_UNIT, property.getInternetMaxSpeedUnit().orElse(null))
          .set(PROPERTIES.INTERNET_STATUS, property.getInternetStatus().orElse(null))
          // Parking
          .set(PROPERTIES.PARKING_SPACES, property.getParkingSpaces().orElse(null))
          .set(PROPERTIES.PARKING_TYPE, property.getParkingType().orElse(null))
          // Safety & Security
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
      // "A vacant property" (etc.) now means "a property with at least one non-deleted unit in
      // that status" — status moved from properties to units in V068.
      condition = condition.and(hasUnitWithStatus(teamId, UnitStatus.valueOf(status)));
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
                  .or(lower(PROPERTIES.IDENTIFIER.cast(String.class)).like(like))
                  .or(lower(PROPERTIES.PROPERTY_TYPE).like(like)));
    }
    Map<String, Field<?>> sortableFields =
        Map.of(
            "createdAt", PROPERTIES.CREATED_AT,
            "updatedAt", PROPERTIES.UPDATED_AT,
            "street", PROPERTIES.STREET,
            "city", PROPERTIES.CITY,
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

  public int countByTeamId(UUID teamId) {
    return dsl.fetchCount(
        dsl.selectFrom(PROPERTIES)
            .where(PROPERTIES.TEAM_ID.eq(teamId).and(PROPERTIES.DELETED_AT.isNull())));
  }

  /**
   * Grouped unit total/occupied counts for every property of {@code teamId}, in one query — so
   * rendering a property list of N properties never fires N per-property count queries. Excludes
   * units of soft-deleted properties, matching {@link UnitRepository}'s own listing/count methods.
   * A property with zero units (which should not normally happen — see {@link
   * com.buurman.service.UnitService}) is simply absent from the map; callers default to zero.
   */
  public Map<UUID, UnitCounts> findUnitCountsByTeamId(UUID teamId) {
    Map<UUID, UnitCounts> counts = new HashMap<>();
    dsl.select(
            UNITS.PROPERTY_ID,
            DSL.count(),
            DSL.sum(
                DSL.when(UNITS.STATUS.eq(UnitStatus.OCCUPIED.name()), DSL.inline(1))
                    .otherwise(DSL.inline(0))),
            DSL.sum(
                DSL.when(UNITS.STATUS.eq(UnitStatus.VACANT.name()), DSL.inline(1))
                    .otherwise(DSL.inline(0))))
        .from(UNITS)
        .join(PROPERTIES)
        .on(PROPERTIES.ID.eq(UNITS.PROPERTY_ID))
        .where(PROPERTIES.TEAM_ID.eq(teamId).and(UNITS.TEAM_ID.eq(teamId)).and(UnitScope.active()))
        .groupBy(UNITS.PROPERTY_ID)
        .fetch()
        .forEach(
            (Record4<UUID, Integer, BigDecimal, BigDecimal> r) -> {
              int occupied = r.value3() == null ? 0 : r.value3().intValue();
              int vacant = r.value4() == null ? 0 : r.value4().intValue();
              counts.put(r.value1(), new UnitCounts(r.value2(), occupied, vacant));
            });
    return counts;
  }

  /**
   * Per-property unit totals for {@link #findUnitCountsByTeamId}. {@code vacant} is a literal count
   * of {@code status == VACANT} units — computed by the same conditional-aggregate pattern as
   * {@code occupied}, in the same single grouped query — so it agrees exactly with
   * PropertyService's single-property unit-facts path (which counts the same way from a unit list)
   * rather than being approximated as {@code total - occupied} (which would silently fold
   * MAINTENANCE/UNAVAILABLE/etc. units into "vacant").
   */
  public record UnitCounts(int total, int occupied, int vacant) {}

  private Condition hasUnitWithStatus(UUID teamId, UnitStatus status) {
    // "A vacant property" (etc.): the property has at least one non-deleted unit in that status.
    // Deliberate product decision (not an obvious 1:1 translation of the old properties.status
    // column) now that a property may hold several independently-let units. UNITS.TEAM_ID is
    // filtered explicitly, matching the codebase-wide rule that every repository query filters
    // team_id itself rather than relying solely on the PROPERTIES.ID join staying team-scoped.
    return DSL.exists(
        DSL.selectOne()
            .from(UNITS)
            .where(
                UNITS
                    .PROPERTY_ID
                    .eq(PROPERTIES.ID)
                    .and(UNITS.TEAM_ID.eq(teamId))
                    .and(UNITS.STATUS.eq(status.name()))
                    .and(UnitScope.active())));
  }
}
