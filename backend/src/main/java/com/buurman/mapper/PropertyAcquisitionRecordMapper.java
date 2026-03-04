package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.PropertyAcquisition;
import com.buurman.jooq.generated.tables.records.PropertyAcquisitionsRecord;
import com.buurman.util.CurrencyUtils;

@Component
public class PropertyAcquisitionRecordMapper {

  public Optional<PropertyAcquisition> toDomain(@Nullable PropertyAcquisitionsRecord record) {
    if (record == null) {
      return Optional.empty();
    }

    PropertyAcquisition acq = new PropertyAcquisition();
    acq.setId(record.getId());
    acq.setIdentifier(java.util.Optional.of(record.getIdentifier()));
    acq.setPropertyId(record.getPropertyId());
    acq.setTeamId(record.getTeamId());
    acq.setAcquisitionType(
        PropertyAcquisition.AcquisitionType.valueOf(record.getAcquisitionType()));
    acq.setAcquisitionDate(Optional.ofNullable(record.getAcquisitionDate()));
    acq.setPurchasePrice(
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(
                record.getPurchasePrice(), record.getPurchasePriceCurrency())));
    acq.setPurchasePriceCurrency(Optional.ofNullable(record.getPurchasePriceCurrency()));
    acq.setClosingCosts(
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(
                record.getClosingCosts(), record.getClosingCostsCurrency())));
    acq.setClosingCostsCurrency(Optional.ofNullable(record.getClosingCostsCurrency()));
    acq.setRenovationCosts(
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(
                record.getRenovationCosts(), record.getRenovationCostsCurrency())));
    acq.setRenovationCostsCurrency(Optional.ofNullable(record.getRenovationCostsCurrency()));
    acq.setLandValue(
        Optional.ofNullable(
            CurrencyUtils.toMajorUnitsOrNull(
                record.getLandValue(), record.getLandValueCurrency())));
    acq.setLandValueCurrency(Optional.ofNullable(record.getLandValueCurrency()));
    acq.setDepreciationMethod(
        Optional.ofNullable(record.getDepreciationMethod())
            .map(PropertyAcquisition.DepreciationMethod::valueOf));
    acq.setDepreciationYears(Optional.ofNullable(record.getDepreciationYears()));
    acq.setNotes(Optional.ofNullable(record.getNotes()));
    acq.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    acq.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    acq.setCreatedBy(record.getCreatedBy());
    acq.setUpdatedBy(record.getUpdatedBy());
    acq.setDeletedAt(Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));

    return Optional.of(acq);
  }
}
