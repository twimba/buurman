package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.PropertyAcquisition;
import com.buurman.jooq.generated.tables.records.PropertyAcquisitionsRecord;
import com.buurman.util.MoneyAmount;

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
        MoneyAmount.ofNullable(record.getPurchasePrice(), record.getPurchasePriceCurrency()));
    acq.setClosingCosts(
        MoneyAmount.ofNullable(record.getClosingCosts(), record.getClosingCostsCurrency()));
    acq.setRenovationCosts(
        MoneyAmount.ofNullable(record.getRenovationCosts(), record.getRenovationCostsCurrency()));
    acq.setLandValue(MoneyAmount.ofNullable(record.getLandValue(), record.getLandValueCurrency()));
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
