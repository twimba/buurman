package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.PropertyOccupancyPeriod;
import com.buurman.domain.PropertyOccupancyPeriod.OccupancyEndReason;
import com.buurman.domain.PropertyOccupancyPeriod.OccupancyType;
import com.buurman.jooq.generated.tables.records.PropertyOccupancyPeriodsRecord;

@Component
public class PropertyOccupancyPeriodRecordMapper {

  public Optional<PropertyOccupancyPeriod> toDomain(
      @Nullable PropertyOccupancyPeriodsRecord record) {
    if (record == null) {
      return Optional.empty();
    }

    PropertyOccupancyPeriod period = new PropertyOccupancyPeriod();
    period.setId(record.getId());
    period.setIdentifier(record.getIdentifier());
    period.setTeamId(record.getTeamId());
    period.setPropertyId(record.getPropertyId());
    period.setStartDate(record.getStartDate());
    period.setEndDate(Optional.ofNullable(record.getEndDate()));
    period.setType(OccupancyType.valueOf(record.getType()));
    period.setOccupantName(Optional.ofNullable(record.getOccupantName()));
    period.setMonthlyImputedRent(Optional.ofNullable(record.getMonthlyImputedRent()));
    period.setEndReason(
        Optional.ofNullable(record.getEndReason()).map(OccupancyEndReason::valueOf));
    period.setNotes(Optional.ofNullable(record.getNotes()));
    period.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    period.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    period.setCreatedBy(record.getCreatedBy());
    period.setUpdatedBy(record.getUpdatedBy());
    period.setDeletedAt(Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));

    return Optional.of(period);
  }
}
