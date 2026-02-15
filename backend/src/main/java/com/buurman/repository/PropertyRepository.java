package com.buurman.repository;

import com.buurman.domain.Property;
import com.buurman.dto.request.PageRequest;
import com.buurman.mapper.PropertyRecordMapper;
import com.buurman.util.PaginationHelper;
import com.buurman.util.PaginationHelper.PaginatedResult;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.springframework.stereotype.Repository;

import com.buurman.jooq.generated.tables.records.PropertiesRecord;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static java.time.ZoneOffset.UTC;

@Repository
@RequiredArgsConstructor
public class PropertyRepository {

    private final DSLContext dsl;
    private final PropertyRecordMapper mapper;
    private final Clock clock;

    public Optional<Property> findByIdentifierAndTeamId(String identifier, UUID teamId) {
        return dsl.selectFrom(PROPERTIES)
                .where(PROPERTIES.IDENTIFIER.eq(identifier)
                        .and(PROPERTIES.TEAM_ID.eq(teamId))
                        .and(PROPERTIES.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public Optional<Property> findByIdAndTeamId(UUID id, UUID teamId) {
        return dsl.selectFrom(PROPERTIES)
                .where(PROPERTIES.ID.eq(id)
                        .and(PROPERTIES.TEAM_ID.eq(teamId))
                        .and(PROPERTIES.DELETED_AT.isNull()))
                .fetchOptional()
                .map(mapper::toDomain);
    }

    public List<Property> findAllByTeamId(UUID teamId) {
        return dsl.selectFrom(PROPERTIES)
                .where(PROPERTIES.TEAM_ID.eq(teamId)
                        .and(PROPERTIES.DELETED_AT.isNull()))
                .orderBy(PROPERTIES.CREATED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public List<Property> findByTeamIdAndStatus(UUID teamId, Property.PropertyStatus status) {
        return dsl.selectFrom(PROPERTIES)
                .where(PROPERTIES.TEAM_ID.eq(teamId)
                        .and(PROPERTIES.STATUS.eq(status.name()))
                        .and(PROPERTIES.DELETED_AT.isNull()))
                .orderBy(PROPERTIES.CREATED_AT.desc())
                .fetch()
                .map(mapper::toDomain);
    }

    public Property save(Property property) {
        LocalDateTime now = LocalDateTime.now(clock);

        if (property.getId() == null) {
            // INSERT
            UUID newId = UUID.randomUUID();
            LocalDateTime createdAt = property.getCreatedAt() != null
                    ? LocalDateTime.ofInstant(property.getCreatedAt(), UTC)
                    : now;
            LocalDateTime updatedAt = property.getUpdatedAt() != null
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
                    .set(PROPERTIES.LATITUDE, property.getLatitude())
                    .set(PROPERTIES.LONGITUDE, property.getLongitude())
                    .set(PROPERTIES.BEDROOMS, property.getBedrooms())
                    .set(PROPERTIES.BATHROOMS, property.getBathrooms())
                    .set(PROPERTIES.AREA_VALUE, property.getAreaValue())
                    .set(PROPERTIES.AREA_UNIT, property.getAreaUnit())
                    .set(PROPERTIES.PROPERTY_TYPE, property.getPropertyType().name())
                    .set(PROPERTIES.STATUS, property.getStatus().name())
                    // Construction & Structure
                    .set(PROPERTIES.YEAR_BUILT, property.getYearBuilt())
                    .set(PROPERTIES.YEAR_LAST_RENOVATED, property.getYearLastRenovated())
                    .set(PROPERTIES.CONSTRUCTION_TYPE, property.getConstructionType())
                    .set(PROPERTIES.FOUNDATION_TYPE, property.getFoundationType())
                    .set(PROPERTIES.ROOF_TYPE, property.getRoofType())
                    .set(PROPERTIES.WALL_CONSTRUCTION, property.getWallConstruction())
                    .set(PROPERTIES.FLOORING_TYPE, property.getFlooringType())
                    .set(PROPERTIES.WINDOW_TYPE, property.getWindowType())
                    .set(PROPERTIES.NUMBER_OF_FLOORS, property.getNumberOfFloors())
                    .set(PROPERTIES.STRUCTURAL_NOTES, property.getStructuralNotes())
                    // Energy & Climate
                    .set(PROPERTIES.ENERGY_EFFICIENCY_RATING, property.getEnergyEfficiencyRating())
                    .set(PROPERTIES.ENERGY_CERTIFICATE_EXPIRY_DATE, property.getEnergyCertificateExpiryDate())
                    .set(PROPERTIES.HEATING_TYPE, property.getHeatingType())
                    .set(PROPERTIES.COOLING_TYPE, property.getCoolingType())
                    .set(PROPERTIES.HOT_WATER_SYSTEM, property.getHotWaterSystem())
                    .set(PROPERTIES.INSULATION_NOTES, property.getInsulationNotes())
                    // Utilities & Connections
                    .set(PROPERTIES.ELECTRICITY_CONNECTION_TYPE, property.getElectricityConnectionType())
                    .set(PROPERTIES.ELECTRICITY_CAPACITY_AMPS, property.getElectricityCapacityAmps())
                    .set(PROPERTIES.WATER_CONNECTION_TYPE, property.getWaterConnectionType())
                    .set(PROPERTIES.HAS_GAS_CONNECTION, property.getHasGasConnection())
                    .set(PROPERTIES.SEWAGE_TYPE, property.getSewageType())
                    .set(PROPERTIES.INTERNET_CONNECTION_TYPE, property.getInternetConnectionType())
                    .set(PROPERTIES.INTERNET_MAX_SPEED_MBPS, property.getInternetMaxSpeedMbps())
                    .set(PROPERTIES.INTERNET_STATUS, property.getInternetStatus())
                    // Parking
                    .set(PROPERTIES.PARKING_SPACES, property.getParkingSpaces())
                    .set(PROPERTIES.PARKING_TYPE, property.getParkingType())
                    // Safety & Security
                    .set(PROPERTIES.HAS_SMOKE_DETECTORS, property.getHasSmokeDetectors())
                    .set(PROPERTIES.HAS_CO_DETECTORS, property.getHasCoDetectors())
                    .set(PROPERTIES.HAS_FIRE_EXTINGUISHER, property.getHasFireExtinguisher())
                    .set(PROPERTIES.HAS_SPRINKLER_SYSTEM, property.getHasSprinklerSystem())
                    .set(PROPERTIES.HAS_ALARM_SYSTEM, property.getHasAlarmSystem())
                    .set(PROPERTIES.HAS_SECURITY_CAMERAS, property.getHasSecurityCameras())
                    .set(PROPERTIES.HAS_SECURE_ENTRY, property.getHasSecureEntry())
                    .set(PROPERTIES.SAFETY_NOTES, property.getSafetyNotes())
                    // Accessibility
                    .set(PROPERTIES.IS_WHEELCHAIR_ACCESSIBLE, property.getIsWheelchairAccessible())
                    .set(PROPERTIES.HAS_ELEVATOR, property.getHasElevator())
                    .set(PROPERTIES.HAS_STEP_FREE_ENTRANCE, property.getHasStepFreeEntrance())
                    .set(PROPERTIES.HAS_ADAPTED_BATHROOM, property.getHasAdaptedBathroom())
                    .set(PROPERTIES.ACCESSIBILITY_NOTES, property.getAccessibilityNotes())
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
            LocalDateTime updatedAt = property.getUpdatedAt() != null
                    ? LocalDateTime.ofInstant(property.getUpdatedAt(), UTC)
                    : now;

            dsl.update(PROPERTIES)
                    .set(PROPERTIES.STREET, property.getStreet())
                    .set(PROPERTIES.CITY, property.getCity())
                    .set(PROPERTIES.POSTAL_CODE, property.getPostalCode())
                    .set(PROPERTIES.COUNTRY, property.getCountry())
                    .set(PROPERTIES.LATITUDE, property.getLatitude())
                    .set(PROPERTIES.LONGITUDE, property.getLongitude())
                    .set(PROPERTIES.BEDROOMS, property.getBedrooms())
                    .set(PROPERTIES.BATHROOMS, property.getBathrooms())
                    .set(PROPERTIES.AREA_VALUE, property.getAreaValue())
                    .set(PROPERTIES.AREA_UNIT, property.getAreaUnit())
                    .set(PROPERTIES.PROPERTY_TYPE, property.getPropertyType().name())
                    .set(PROPERTIES.STATUS, property.getStatus().name())
                    // Construction & Structure
                    .set(PROPERTIES.YEAR_BUILT, property.getYearBuilt())
                    .set(PROPERTIES.YEAR_LAST_RENOVATED, property.getYearLastRenovated())
                    .set(PROPERTIES.CONSTRUCTION_TYPE, property.getConstructionType())
                    .set(PROPERTIES.FOUNDATION_TYPE, property.getFoundationType())
                    .set(PROPERTIES.ROOF_TYPE, property.getRoofType())
                    .set(PROPERTIES.WALL_CONSTRUCTION, property.getWallConstruction())
                    .set(PROPERTIES.FLOORING_TYPE, property.getFlooringType())
                    .set(PROPERTIES.WINDOW_TYPE, property.getWindowType())
                    .set(PROPERTIES.NUMBER_OF_FLOORS, property.getNumberOfFloors())
                    .set(PROPERTIES.STRUCTURAL_NOTES, property.getStructuralNotes())
                    // Energy & Climate
                    .set(PROPERTIES.ENERGY_EFFICIENCY_RATING, property.getEnergyEfficiencyRating())
                    .set(PROPERTIES.ENERGY_CERTIFICATE_EXPIRY_DATE, property.getEnergyCertificateExpiryDate())
                    .set(PROPERTIES.HEATING_TYPE, property.getHeatingType())
                    .set(PROPERTIES.COOLING_TYPE, property.getCoolingType())
                    .set(PROPERTIES.HOT_WATER_SYSTEM, property.getHotWaterSystem())
                    .set(PROPERTIES.INSULATION_NOTES, property.getInsulationNotes())
                    // Utilities & Connections
                    .set(PROPERTIES.ELECTRICITY_CONNECTION_TYPE, property.getElectricityConnectionType())
                    .set(PROPERTIES.ELECTRICITY_CAPACITY_AMPS, property.getElectricityCapacityAmps())
                    .set(PROPERTIES.WATER_CONNECTION_TYPE, property.getWaterConnectionType())
                    .set(PROPERTIES.HAS_GAS_CONNECTION, property.getHasGasConnection())
                    .set(PROPERTIES.SEWAGE_TYPE, property.getSewageType())
                    .set(PROPERTIES.INTERNET_CONNECTION_TYPE, property.getInternetConnectionType())
                    .set(PROPERTIES.INTERNET_MAX_SPEED_MBPS, property.getInternetMaxSpeedMbps())
                    .set(PROPERTIES.INTERNET_STATUS, property.getInternetStatus())
                    // Parking
                    .set(PROPERTIES.PARKING_SPACES, property.getParkingSpaces())
                    .set(PROPERTIES.PARKING_TYPE, property.getParkingType())
                    // Safety & Security
                    .set(PROPERTIES.HAS_SMOKE_DETECTORS, property.getHasSmokeDetectors())
                    .set(PROPERTIES.HAS_CO_DETECTORS, property.getHasCoDetectors())
                    .set(PROPERTIES.HAS_FIRE_EXTINGUISHER, property.getHasFireExtinguisher())
                    .set(PROPERTIES.HAS_SPRINKLER_SYSTEM, property.getHasSprinklerSystem())
                    .set(PROPERTIES.HAS_ALARM_SYSTEM, property.getHasAlarmSystem())
                    .set(PROPERTIES.HAS_SECURITY_CAMERAS, property.getHasSecurityCameras())
                    .set(PROPERTIES.HAS_SECURE_ENTRY, property.getHasSecureEntry())
                    .set(PROPERTIES.SAFETY_NOTES, property.getSafetyNotes())
                    // Accessibility
                    .set(PROPERTIES.IS_WHEELCHAIR_ACCESSIBLE, property.getIsWheelchairAccessible())
                    .set(PROPERTIES.HAS_ELEVATOR, property.getHasElevator())
                    .set(PROPERTIES.HAS_STEP_FREE_ENTRANCE, property.getHasStepFreeEntrance())
                    .set(PROPERTIES.HAS_ADAPTED_BATHROOM, property.getHasAdaptedBathroom())
                    .set(PROPERTIES.ACCESSIBILITY_NOTES, property.getAccessibilityNotes())
                    // Audit
                    .set(PROPERTIES.UPDATED_AT, updatedAt)
                    .set(PROPERTIES.UPDATED_BY, property.getUpdatedBy())
                    .where(PROPERTIES.ID.eq(property.getId())
                            .and(PROPERTIES.TEAM_ID.eq(property.getTeamId())))
                    .execute();

            property.setUpdatedAt(updatedAt.toInstant(UTC));
        }

        return property;
    }

    public PaginatedResult<Property> findAllByTeamIdPaginated(UUID teamId, String status, PageRequest pageRequest) {
        Condition condition = PROPERTIES.TEAM_ID.eq(teamId).and(PROPERTIES.DELETED_AT.isNull());
        if (status != null && !status.isEmpty()) {
            condition = condition.and(PROPERTIES.STATUS.eq(status));
        }
        Map<String, Field<?>> sortableFields = Map.of(
            "createdAt", PROPERTIES.CREATED_AT,
            "street", PROPERTIES.STREET,
            "city", PROPERTIES.CITY,
            "status", PROPERTIES.STATUS,
            "propertyType", PROPERTIES.PROPERTY_TYPE
        );
        return PaginationHelper.paginate(dsl, PROPERTIES, condition, sortableFields, PROPERTIES.CREATED_AT, pageRequest, r -> mapper.toDomain((PropertiesRecord) r));
    }

    public List<Property> findByIdsAndTeamId(Collection<UUID> ids, UUID teamId) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return dsl.selectFrom(PROPERTIES)
                .where(PROPERTIES.ID.in(ids)
                        .and(PROPERTIES.TEAM_ID.eq(teamId))
                        .and(PROPERTIES.DELETED_AT.isNull()))
                .fetch()
                .map(mapper::toDomain);
    }

    public void softDeleteByIdAndTeamId(UUID id, UUID teamId) {
        LocalDateTime now = LocalDateTime.now(clock);
        dsl.update(PROPERTIES)
                .set(PROPERTIES.DELETED_AT, now)
                .where(PROPERTIES.ID.eq(id)
                        .and(PROPERTIES.TEAM_ID.eq(teamId)))
                .execute();
    }
}
