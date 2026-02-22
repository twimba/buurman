package com.buurman.repository;

import static com.buurman.jooq.generated.Tables.PROPERTIES;
import static java.time.ZoneOffset.UTC;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
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
import com.buurman.jooq.generated.tables.records.PropertiesRecord;
import com.buurman.mapper.PropertyRecordMapper;
import com.buurman.util.CurrencyUtils;
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
          .set(PROPERTIES.LATITUDE, property.getLatitude())
          .set(PROPERTIES.LONGITUDE, property.getLongitude())
          .set(PROPERTIES.GEOCODE_ACCURACY, property.getGeocodeAccuracy())
          .set(PROPERTIES.AREA_VALUE, property.getAreaValue())
          .set(PROPERTIES.AREA_UNIT, property.getAreaUnit())
          .set(PROPERTIES.PROPERTY_CATEGORY, property.getPropertyCategory().name())
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
          // Investment & Financial
          .set(
              PROPERTIES.PURCHASE_PRICE,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getPurchasePrice(), property.getPurchasePriceCurrency()))
          .set(PROPERTIES.PURCHASE_PRICE_CURRENCY, property.getPurchasePriceCurrency())
          .set(PROPERTIES.PURCHASE_DATE, property.getPurchaseDate())
          .set(
              PROPERTIES.CURRENT_MARKET_VALUE,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getCurrentMarketValue(), property.getCurrentMarketValueCurrency()))
          .set(PROPERTIES.CURRENT_MARKET_VALUE_CURRENCY, property.getCurrentMarketValueCurrency())
          .set(PROPERTIES.MARKET_VALUE_DATE, property.getMarketValueDate())
          .set(
              PROPERTIES.MORTGAGE_TYPE,
              property.getMortgageType() != null ? property.getMortgageType().name() : null)
          .set(
              PROPERTIES.MORTGAGE_AMOUNT,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getMortgageAmount(), property.getMortgageAmountCurrency()))
          .set(PROPERTIES.MORTGAGE_AMOUNT_CURRENCY, property.getMortgageAmountCurrency())
          .set(PROPERTIES.MORTGAGE_INTEREST_RATE, property.getMortgageInterestRate())
          .set(PROPERTIES.MORTGAGE_START_DATE, property.getMortgageStartDate())
          .set(PROPERTIES.MORTGAGE_END_DATE, property.getMortgageEndDate())
          .set(
              PROPERTIES.MONTHLY_MORTGAGE_PAYMENT,
              isVariablePayment(property.getMonthlyMortgagePayment())
                  ? Property.VARIABLE_PAYMENT_SENTINEL_DB
                  : CurrencyUtils.toMinorUnitsOrNull(
                      property.getMonthlyMortgagePayment(),
                      property.getMonthlyMortgagePaymentCurrency()))
          .set(
              PROPERTIES.MONTHLY_MORTGAGE_PAYMENT_CURRENCY,
              isVariablePayment(property.getMonthlyMortgagePayment())
                  ? null
                  : property.getMonthlyMortgagePaymentCurrency())
          .set(
              PROPERTIES.ANNUAL_PROPERTY_TAX,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getAnnualPropertyTax(), property.getAnnualPropertyTaxCurrency()))
          .set(PROPERTIES.ANNUAL_PROPERTY_TAX_CURRENCY, property.getAnnualPropertyTaxCurrency())
          .set(
              PROPERTIES.ANNUAL_INSURANCE,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getAnnualInsurance(), property.getAnnualInsuranceCurrency()))
          .set(PROPERTIES.ANNUAL_INSURANCE_CURRENCY, property.getAnnualInsuranceCurrency())
          .set(
              PROPERTIES.ANNUAL_HOA_FEE,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getAnnualHoaFee(), property.getAnnualHoaFeeCurrency()))
          .set(PROPERTIES.ANNUAL_HOA_FEE_CURRENCY, property.getAnnualHoaFeeCurrency())
          .set(
              PROPERTIES.ANNUAL_MANAGEMENT_FEE,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getAnnualManagementFee(), property.getAnnualManagementFeeCurrency()))
          .set(PROPERTIES.ANNUAL_MANAGEMENT_FEE_CURRENCY, property.getAnnualManagementFeeCurrency())
          .set(
              PROPERTIES.ANNUAL_MAINTENANCE_RESERVE,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getAnnualMaintenanceReserve(),
                  property.getAnnualMaintenanceReserveCurrency()))
          .set(
              PROPERTIES.ANNUAL_MAINTENANCE_RESERVE_CURRENCY,
              property.getAnnualMaintenanceReserveCurrency())
          .set(PROPERTIES.ANNUAL_PROPERTY_TAX_DUE_MONTH, property.getAnnualPropertyTaxDueMonth())
          .set(PROPERTIES.ANNUAL_INSURANCE_DUE_MONTH, property.getAnnualInsuranceDueMonth())
          .set(PROPERTIES.ANNUAL_HOA_FEE_DUE_MONTH, property.getAnnualHoaFeeDueMonth())
          .set(
              PROPERTIES.ANNUAL_MANAGEMENT_FEE_DUE_MONTH, property.getAnnualManagementFeeDueMonth())
          .set(
              PROPERTIES.ANNUAL_MAINTENANCE_RESERVE_DUE_MONTH,
              property.getAnnualMaintenanceReserveDueMonth())
          .set(
              PROPERTIES.DEPRECIATION_METHOD,
              property.getDepreciationMethod() != null
                  ? property.getDepreciationMethod().name()
                  : null)
          .set(PROPERTIES.DEPRECIATION_YEARS, property.getDepreciationYears())
          .set(
              PROPERTIES.LAND_VALUE,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getLandValue(), property.getLandValueCurrency()))
          .set(PROPERTIES.LAND_VALUE_CURRENCY, property.getLandValueCurrency())
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
          .set(PROPERTIES.LATITUDE, property.getLatitude())
          .set(PROPERTIES.LONGITUDE, property.getLongitude())
          .set(PROPERTIES.GEOCODE_ACCURACY, property.getGeocodeAccuracy())
          .set(PROPERTIES.AREA_VALUE, property.getAreaValue())
          .set(PROPERTIES.AREA_UNIT, property.getAreaUnit())
          .set(PROPERTIES.PROPERTY_TYPE, property.getPropertyType().name())
          .set(PROPERTIES.STATUS, property.getStatus().name())
          // Note: property_category is NOT updated (immutable)
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
          // Investment & Financial
          .set(
              PROPERTIES.PURCHASE_PRICE,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getPurchasePrice(), property.getPurchasePriceCurrency()))
          .set(PROPERTIES.PURCHASE_PRICE_CURRENCY, property.getPurchasePriceCurrency())
          .set(PROPERTIES.PURCHASE_DATE, property.getPurchaseDate())
          .set(
              PROPERTIES.CURRENT_MARKET_VALUE,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getCurrentMarketValue(), property.getCurrentMarketValueCurrency()))
          .set(PROPERTIES.CURRENT_MARKET_VALUE_CURRENCY, property.getCurrentMarketValueCurrency())
          .set(PROPERTIES.MARKET_VALUE_DATE, property.getMarketValueDate())
          .set(
              PROPERTIES.MORTGAGE_TYPE,
              property.getMortgageType() != null ? property.getMortgageType().name() : null)
          .set(
              PROPERTIES.MORTGAGE_AMOUNT,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getMortgageAmount(), property.getMortgageAmountCurrency()))
          .set(PROPERTIES.MORTGAGE_AMOUNT_CURRENCY, property.getMortgageAmountCurrency())
          .set(PROPERTIES.MORTGAGE_INTEREST_RATE, property.getMortgageInterestRate())
          .set(PROPERTIES.MORTGAGE_START_DATE, property.getMortgageStartDate())
          .set(PROPERTIES.MORTGAGE_END_DATE, property.getMortgageEndDate())
          .set(
              PROPERTIES.MONTHLY_MORTGAGE_PAYMENT,
              isVariablePayment(property.getMonthlyMortgagePayment())
                  ? Property.VARIABLE_PAYMENT_SENTINEL_DB
                  : CurrencyUtils.toMinorUnitsOrNull(
                      property.getMonthlyMortgagePayment(),
                      property.getMonthlyMortgagePaymentCurrency()))
          .set(
              PROPERTIES.MONTHLY_MORTGAGE_PAYMENT_CURRENCY,
              isVariablePayment(property.getMonthlyMortgagePayment())
                  ? null
                  : property.getMonthlyMortgagePaymentCurrency())
          .set(
              PROPERTIES.ANNUAL_PROPERTY_TAX,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getAnnualPropertyTax(), property.getAnnualPropertyTaxCurrency()))
          .set(PROPERTIES.ANNUAL_PROPERTY_TAX_CURRENCY, property.getAnnualPropertyTaxCurrency())
          .set(
              PROPERTIES.ANNUAL_INSURANCE,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getAnnualInsurance(), property.getAnnualInsuranceCurrency()))
          .set(PROPERTIES.ANNUAL_INSURANCE_CURRENCY, property.getAnnualInsuranceCurrency())
          .set(
              PROPERTIES.ANNUAL_HOA_FEE,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getAnnualHoaFee(), property.getAnnualHoaFeeCurrency()))
          .set(PROPERTIES.ANNUAL_HOA_FEE_CURRENCY, property.getAnnualHoaFeeCurrency())
          .set(
              PROPERTIES.ANNUAL_MANAGEMENT_FEE,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getAnnualManagementFee(), property.getAnnualManagementFeeCurrency()))
          .set(PROPERTIES.ANNUAL_MANAGEMENT_FEE_CURRENCY, property.getAnnualManagementFeeCurrency())
          .set(
              PROPERTIES.ANNUAL_MAINTENANCE_RESERVE,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getAnnualMaintenanceReserve(),
                  property.getAnnualMaintenanceReserveCurrency()))
          .set(
              PROPERTIES.ANNUAL_MAINTENANCE_RESERVE_CURRENCY,
              property.getAnnualMaintenanceReserveCurrency())
          .set(PROPERTIES.ANNUAL_PROPERTY_TAX_DUE_MONTH, property.getAnnualPropertyTaxDueMonth())
          .set(PROPERTIES.ANNUAL_INSURANCE_DUE_MONTH, property.getAnnualInsuranceDueMonth())
          .set(PROPERTIES.ANNUAL_HOA_FEE_DUE_MONTH, property.getAnnualHoaFeeDueMonth())
          .set(
              PROPERTIES.ANNUAL_MANAGEMENT_FEE_DUE_MONTH, property.getAnnualManagementFeeDueMonth())
          .set(
              PROPERTIES.ANNUAL_MAINTENANCE_RESERVE_DUE_MONTH,
              property.getAnnualMaintenanceReserveDueMonth())
          .set(
              PROPERTIES.DEPRECIATION_METHOD,
              property.getDepreciationMethod() != null
                  ? property.getDepreciationMethod().name()
                  : null)
          .set(PROPERTIES.DEPRECIATION_YEARS, property.getDepreciationYears())
          .set(
              PROPERTIES.LAND_VALUE,
              CurrencyUtils.toMinorUnitsOrNull(
                  property.getLandValue(), property.getLandValueCurrency()))
          .set(PROPERTIES.LAND_VALUE_CURRENCY, property.getLandValueCurrency())
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
      String like = "%" + query.trim().toLowerCase() + "%";
      condition =
          condition.and(
              PROPERTIES
                  .STREET
                  .lower()
                  .like(like)
                  .or(PROPERTIES.CITY.lower().like(like))
                  .or(PROPERTIES.POSTAL_CODE.lower().like(like))
                  .or(PROPERTIES.IDENTIFIER.lower().like(like))
                  .or(PROPERTIES.PROPERTY_TYPE.lower().like(like)));
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
        r -> mapper.toDomain((PropertiesRecord) r));
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

  private static boolean isVariablePayment(@Nullable java.math.BigDecimal value) {
    return value != null && value.compareTo(Property.VARIABLE_PAYMENT_SENTINEL) == 0;
  }
}
