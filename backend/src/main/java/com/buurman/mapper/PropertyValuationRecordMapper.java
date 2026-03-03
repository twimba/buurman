package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.PropertyValuation;
import com.buurman.jooq.generated.tables.records.PropertyValuationsRecord;
import com.buurman.util.CurrencyUtils;

@Component
public class PropertyValuationRecordMapper {

  public Optional<PropertyValuation> toDomain(@Nullable PropertyValuationsRecord record) {
    if (record == null) {
      return Optional.empty();
    }

    PropertyValuation val = new PropertyValuation();
    val.setId(record.getId());
    val.setIdentifier(java.util.Optional.of(record.getIdentifier()));
    val.setPropertyId(record.getPropertyId());
    val.setTeamId(record.getTeamId());
    val.setValuationType(PropertyValuation.ValuationType.valueOf(record.getValuationType()));
    val.setValuationDate(record.getValuationDate());
    val.setAmount(CurrencyUtils.toMajorUnits(record.getAmount(), record.getCurrency()));
    val.setCurrency(record.getCurrency());
    val.setSource(Optional.ofNullable(record.getSource()));
    val.setNotes(Optional.ofNullable(record.getNotes()));
    val.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    val.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    val.setCreatedBy(record.getCreatedBy());
    val.setUpdatedBy(record.getUpdatedBy());
    val.setDeletedAt(Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));

    return Optional.of(val);
  }
}
