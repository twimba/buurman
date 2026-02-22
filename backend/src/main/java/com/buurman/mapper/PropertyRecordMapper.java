package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import org.jspecify.annotations.Nullable;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.buurman.domain.Property;
import com.buurman.jooq.generated.tables.records.PropertiesRecord;
import com.buurman.util.CurrencyUtils;

@Mapper(componentModel = "spring")
public interface PropertyRecordMapper {

  @Mapping(
      target = "propertyCategory",
      expression = "java(toPropertyCategory(record.getPropertyCategory()))")
  @Mapping(target = "propertyType", expression = "java(toPropertyType(record.getPropertyType()))")
  @Mapping(target = "status", expression = "java(toPropertyStatus(record.getStatus()))")
  @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
  @Mapping(target = "deletedAt", expression = "java(toInstant(record.getDeletedAt()))")
  @Mapping(target = "mortgageType", expression = "java(toMortgageType(record.getMortgageType()))")
  @Mapping(
      target = "depreciationMethod",
      expression = "java(toDepreciationMethod(record.getDepreciationMethod()))")
  // Monetary fields: ignore auto-mapping (Long→BigDecimal is wrong); set via @AfterMapping
  @Mapping(target = "purchasePrice", ignore = true)
  @Mapping(target = "currentMarketValue", ignore = true)
  @Mapping(target = "mortgageAmount", ignore = true)
  @Mapping(target = "monthlyMortgagePayment", ignore = true)
  @Mapping(target = "annualPropertyTax", ignore = true)
  @Mapping(target = "annualInsurance", ignore = true)
  @Mapping(target = "annualHoaFee", ignore = true)
  @Mapping(target = "annualManagementFee", ignore = true)
  @Mapping(target = "annualMaintenanceReserve", ignore = true)
  @Mapping(target = "landValue", ignore = true)
  Property toDomain(PropertiesRecord record);

  @Mapping(
      target = "propertyCategory",
      expression = "java(fromPropertyCategory(property.getPropertyCategory()))")
  @Mapping(
      target = "propertyType",
      expression = "java(fromPropertyType(property.getPropertyType()))")
  @Mapping(target = "status", expression = "java(fromPropertyStatus(property.getStatus()))")
  @Mapping(target = "createdAt", expression = "java(toLocalDateTime(property.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toLocalDateTime(property.getUpdatedAt()))")
  @Mapping(target = "deletedAt", expression = "java(toLocalDateTime(property.getDeletedAt()))")
  @Mapping(
      target = "mortgageType",
      expression = "java(fromMortgageType(property.getMortgageType()))")
  @Mapping(
      target = "depreciationMethod",
      expression = "java(fromDepreciationMethod(property.getDepreciationMethod()))")
  PropertiesRecord toRecord(Property property);

  List<Property> toDomainList(List<PropertiesRecord> records);

  @AfterMapping
  default void convertMonetaryFields(PropertiesRecord record, @MappingTarget Property property) {
    property.setPurchasePrice(
        CurrencyUtils.toMajorUnitsOrNull(
            record.getPurchasePrice(), record.getPurchasePriceCurrency()));
    property.setPurchasePriceCurrency(record.getPurchasePriceCurrency());
    property.setCurrentMarketValue(
        CurrencyUtils.toMajorUnitsOrNull(
            record.getCurrentMarketValue(), record.getCurrentMarketValueCurrency()));
    property.setCurrentMarketValueCurrency(record.getCurrentMarketValueCurrency());
    property.setMortgageAmount(
        CurrencyUtils.toMajorUnitsOrNull(
            record.getMortgageAmount(), record.getMortgageAmountCurrency()));
    property.setMortgageAmountCurrency(record.getMortgageAmountCurrency());
    if (record.getMonthlyMortgagePayment() != null
        && record.getMonthlyMortgagePayment() == Property.VARIABLE_PAYMENT_SENTINEL_DB) {
      property.setMonthlyMortgagePayment(Property.VARIABLE_PAYMENT_SENTINEL);
      property.setMonthlyMortgagePaymentCurrency(null);
    } else {
      property.setMonthlyMortgagePayment(
          CurrencyUtils.toMajorUnitsOrNull(
              record.getMonthlyMortgagePayment(), record.getMonthlyMortgagePaymentCurrency()));
      property.setMonthlyMortgagePaymentCurrency(record.getMonthlyMortgagePaymentCurrency());
    }
    property.setAnnualPropertyTax(
        CurrencyUtils.toMajorUnitsOrNull(
            record.getAnnualPropertyTax(), record.getAnnualPropertyTaxCurrency()));
    property.setAnnualPropertyTaxCurrency(record.getAnnualPropertyTaxCurrency());
    property.setAnnualInsurance(
        CurrencyUtils.toMajorUnitsOrNull(
            record.getAnnualInsurance(), record.getAnnualInsuranceCurrency()));
    property.setAnnualInsuranceCurrency(record.getAnnualInsuranceCurrency());
    property.setAnnualHoaFee(
        CurrencyUtils.toMajorUnitsOrNull(
            record.getAnnualHoaFee(), record.getAnnualHoaFeeCurrency()));
    property.setAnnualHoaFeeCurrency(record.getAnnualHoaFeeCurrency());
    property.setAnnualManagementFee(
        CurrencyUtils.toMajorUnitsOrNull(
            record.getAnnualManagementFee(), record.getAnnualManagementFeeCurrency()));
    property.setAnnualManagementFeeCurrency(record.getAnnualManagementFeeCurrency());
    property.setAnnualMaintenanceReserve(
        CurrencyUtils.toMajorUnitsOrNull(
            record.getAnnualMaintenanceReserve(), record.getAnnualMaintenanceReserveCurrency()));
    property.setAnnualMaintenanceReserveCurrency(record.getAnnualMaintenanceReserveCurrency());
    property.setLandValue(
        CurrencyUtils.toMajorUnitsOrNull(record.getLandValue(), record.getLandValueCurrency()));
    property.setLandValueCurrency(record.getLandValueCurrency());
  }

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }

  default @Nullable LocalDateTime toLocalDateTime(@Nullable Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, UTC);
  }

  default Property.@Nullable PropertyCategory toPropertyCategory(@Nullable String value) {
    return value == null ? null : Property.PropertyCategory.valueOf(value);
  }

  default @Nullable String fromPropertyCategory(Property.@Nullable PropertyCategory category) {
    return category == null ? null : category.name();
  }

  default Property.@Nullable PropertyType toPropertyType(@Nullable String value) {
    return value == null ? null : Property.PropertyType.valueOf(value);
  }

  default @Nullable String fromPropertyType(Property.@Nullable PropertyType type) {
    return type == null ? null : type.name();
  }

  default Property.@Nullable PropertyStatus toPropertyStatus(@Nullable String value) {
    return value == null ? null : Property.PropertyStatus.valueOf(value);
  }

  default @Nullable String fromPropertyStatus(Property.@Nullable PropertyStatus status) {
    return status == null ? null : status.name();
  }

  default Property.@Nullable MortgageType toMortgageType(@Nullable String value) {
    return value == null ? null : Property.MortgageType.valueOf(value);
  }

  default @Nullable String fromMortgageType(Property.@Nullable MortgageType type) {
    return type == null ? null : type.name();
  }

  default Property.@Nullable DepreciationMethod toDepreciationMethod(@Nullable String value) {
    return value == null ? null : Property.DepreciationMethod.valueOf(value);
  }

  default @Nullable String fromDepreciationMethod(Property.@Nullable DepreciationMethod method) {
    return method == null ? null : method.name();
  }
}
