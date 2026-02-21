package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

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
    property.setMonthlyMortgagePayment(
        CurrencyUtils.toMajorUnitsOrNull(
            record.getMonthlyMortgagePayment(), record.getMonthlyMortgagePaymentCurrency()));
    property.setMonthlyMortgagePaymentCurrency(record.getMonthlyMortgagePaymentCurrency());
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
        CurrencyUtils.toMajorUnitsOrNull(
            record.getLandValue(), record.getLandValueCurrency()));
    property.setLandValueCurrency(record.getLandValueCurrency());
  }

  default Instant toInstant(LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }

  default LocalDateTime toLocalDateTime(Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, UTC);
  }

  default Property.PropertyCategory toPropertyCategory(String value) {
    return value == null ? null : Property.PropertyCategory.valueOf(value);
  }

  default String fromPropertyCategory(Property.PropertyCategory category) {
    return category == null ? null : category.name();
  }

  default Property.PropertyType toPropertyType(String value) {
    return value == null ? null : Property.PropertyType.valueOf(value);
  }

  default String fromPropertyType(Property.PropertyType type) {
    return type == null ? null : type.name();
  }

  default Property.PropertyStatus toPropertyStatus(String value) {
    return value == null ? null : Property.PropertyStatus.valueOf(value);
  }

  default String fromPropertyStatus(Property.PropertyStatus status) {
    return status == null ? null : status.name();
  }

  default Property.MortgageType toMortgageType(String value) {
    return value == null ? null : Property.MortgageType.valueOf(value);
  }

  default String fromMortgageType(Property.MortgageType type) {
    return type == null ? null : type.name();
  }

  default Property.DepreciationMethod toDepreciationMethod(String value) {
    return value == null ? null : Property.DepreciationMethod.valueOf(value);
  }

  default String fromDepreciationMethod(Property.DepreciationMethod method) {
    return method == null ? null : method.name();
  }
}
