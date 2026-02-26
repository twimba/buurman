package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.PropertyFee;
import com.buurman.jooq.generated.tables.records.PropertyFeesRecord;
import com.buurman.util.CurrencyUtils;

@Component
public class PropertyFeeRecordMapper {

  public Optional<PropertyFee> toDomain(@Nullable PropertyFeesRecord record) {
    if (record == null) {
      return Optional.empty();
    }

    PropertyFee fee = new PropertyFee();
    fee.setId(record.getId());
    fee.setIdentifier(record.getIdentifier());
    fee.setPropertyId(record.getPropertyId());
    fee.setTeamId(record.getTeamId());
    fee.setFeeType(PropertyFee.FeeType.valueOf(record.getFeeType()));
    fee.setName(Optional.ofNullable(record.getName()));
    fee.setAnnualAmount(CurrencyUtils.toMajorUnits(record.getAnnualAmount(), record.getCurrency()));
    fee.setCurrency(record.getCurrency());
    fee.setPaymentFrequency(record.getPaymentFrequency());
    fee.setDueMonths(Optional.ofNullable(record.getDueMonths()));
    fee.setStartDate(Optional.ofNullable(record.getStartDate()));
    fee.setEndDate(Optional.ofNullable(record.getEndDate()));
    fee.setStatus(PropertyFee.FeeStatus.valueOf(record.getStatus()));
    fee.setNotes(Optional.ofNullable(record.getNotes()));
    fee.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    fee.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    fee.setCreatedBy(record.getCreatedBy());
    fee.setUpdatedBy(record.getUpdatedBy());
    fee.setDeletedAt(Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));

    return Optional.of(fee);
  }
}
