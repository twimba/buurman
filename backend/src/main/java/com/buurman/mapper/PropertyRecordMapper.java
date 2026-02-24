package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import com.buurman.domain.Property;
import com.buurman.domain.Property.PropertyCategory;
import com.buurman.domain.Property.PropertyStatus;
import com.buurman.domain.Property.PropertyType;
import com.buurman.jooq.generated.tables.records.PropertiesRecord;
import com.buurman.util.CurrencyUtils;

@Mapper(componentModel = "spring", uses = OptionalMappingConfig.class)
public interface PropertyRecordMapper {

  @Mapping(
      target = "propertyCategory",
      expression = "java(toPropertyCategory(record.getPropertyCategory()))")
  @Mapping(target = "propertyType", expression = "java(toPropertyType(record.getPropertyType()))")
  @Mapping(target = "status", expression = "java(toPropertyStatus(record.getStatus()))")
  @Mapping(target = "createdAt", expression = "java(toInstant(record.getCreatedAt()))")
  @Mapping(target = "updatedAt", expression = "java(toInstant(record.getUpdatedAt()))")
  @Mapping(target = "deletedAt", expression = "java(toOptionalInstant(record.getDeletedAt()))")
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
  @Mapping(target = "deletedAt", expression = "java(fromOptionalInstant(property.getDeletedAt()))")
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
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(
                record.getPurchasePrice(), record.getPurchasePriceCurrency())));
    property.setPurchasePriceCurrency(Optional.ofNullable(record.getPurchasePriceCurrency()));
    property.setCurrentMarketValue(
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(
                record.getCurrentMarketValue(), record.getCurrentMarketValueCurrency())));
    property.setCurrentMarketValueCurrency(
        Optional.ofNullable(record.getCurrentMarketValueCurrency()));
    property.setMortgageAmount(
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(
                record.getMortgageAmount(), record.getMortgageAmountCurrency())));
    property.setMortgageAmountCurrency(Optional.ofNullable(record.getMortgageAmountCurrency()));
    property.setMonthlyMortgagePayment(
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(
                record.getMonthlyMortgagePayment(), record.getMonthlyMortgagePaymentCurrency())));
    property.setMonthlyMortgagePaymentCurrency(
        Optional.ofNullable(record.getMonthlyMortgagePaymentCurrency()));
    property.setAnnualPropertyTax(
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(
                record.getAnnualPropertyTax(), record.getAnnualPropertyTaxCurrency())));
    property.setAnnualPropertyTaxCurrency(
        Optional.ofNullable(record.getAnnualPropertyTaxCurrency()));
    property.setAnnualInsurance(
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(
                record.getAnnualInsurance(), record.getAnnualInsuranceCurrency())));
    property.setAnnualInsuranceCurrency(Optional.ofNullable(record.getAnnualInsuranceCurrency()));
    property.setAnnualHoaFee(
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(
                record.getAnnualHoaFee(), record.getAnnualHoaFeeCurrency())));
    property.setAnnualHoaFeeCurrency(Optional.ofNullable(record.getAnnualHoaFeeCurrency()));
    property.setAnnualManagementFee(
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(
                record.getAnnualManagementFee(), record.getAnnualManagementFeeCurrency())));
    property.setAnnualManagementFeeCurrency(
        Optional.ofNullable(record.getAnnualManagementFeeCurrency()));
    property.setAnnualMaintenanceReserve(
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(
                record.getAnnualMaintenanceReserve(),
                record.getAnnualMaintenanceReserveCurrency())));
    property.setAnnualMaintenanceReserveCurrency(
        Optional.ofNullable(record.getAnnualMaintenanceReserveCurrency()));
    property.setLandValue(
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(
                record.getLandValue(), record.getLandValueCurrency())));
    property.setLandValueCurrency(Optional.ofNullable(record.getLandValueCurrency()));
  }

  default @Nullable Instant toInstant(@Nullable LocalDateTime localDateTime) {
    return localDateTime == null ? null : localDateTime.toInstant(UTC);
  }

  default Optional<Instant> toOptionalInstant(@Nullable LocalDateTime localDateTime) {
    return Optional.ofNullable(localDateTime == null ? null : localDateTime.toInstant(UTC));
  }

  default @Nullable LocalDateTime toLocalDateTime(@Nullable Instant instant) {
    return instant == null ? null : LocalDateTime.ofInstant(instant, UTC);
  }

  default @Nullable LocalDateTime fromOptionalInstant(Optional<Instant> value) {
    return value.map(i -> LocalDateTime.ofInstant(i, UTC)).orElse(null);
  }

  default @Nullable PropertyCategory toPropertyCategory(@Nullable String value) {
    return value == null ? null : PropertyCategory.valueOf(value);
  }

  default @Nullable String fromPropertyCategory(@Nullable PropertyCategory category) {
    return category == null ? null : category.name();
  }

  default @Nullable PropertyType toPropertyType(@Nullable String value) {
    return value == null ? null : PropertyType.valueOf(value);
  }

  default @Nullable String fromPropertyType(@Nullable PropertyType type) {
    return type == null ? null : type.name();
  }

  default @Nullable PropertyStatus toPropertyStatus(@Nullable String value) {
    return value == null ? null : PropertyStatus.valueOf(value);
  }

  default @Nullable String fromPropertyStatus(@Nullable PropertyStatus status) {
    return status == null ? null : status.name();
  }

  default Optional<Property.MortgageType> toMortgageType(@Nullable String value) {
    return Optional.ofNullable(value == null ? null : Property.MortgageType.valueOf(value));
  }

  default @Nullable String fromMortgageType(Optional<Property.MortgageType> type) {
    return type.map(Enum::name).orElse(null);
  }

  default Optional<Property.DepreciationMethod> toDepreciationMethod(@Nullable String value) {
    return Optional.ofNullable(value == null ? null : Property.DepreciationMethod.valueOf(value));
  }

  default @Nullable String fromDepreciationMethod(Optional<Property.DepreciationMethod> method) {
    return method.map(Enum::name).orElse(null);
  }
}
