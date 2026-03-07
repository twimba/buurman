package com.buurman.mapper;

import static java.time.ZoneOffset.UTC;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.buurman.domain.WwsCalculation;
import com.buurman.jooq.generated.tables.records.WwsCalculationsRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class WwsCalculationRecordMapper {

  private final ObjectMapper objectMapper;

  public Optional<WwsCalculation> toDomain(@Nullable WwsCalculationsRecord record) {
    if (record == null) {
      return Optional.empty();
    }

    List<WwsCalculation.CategoryBreakdown> breakdown = List.of();
    try {
      breakdown =
          objectMapper.readValue(record.getCategoryBreakdown().data(), new TypeReference<>() {});
    } catch (Exception e) {
      // Log but don't fail — breakdown is informational
    }

    WwsCalculation calc = new WwsCalculation();
    calc.setId(record.getId());
    calc.setIdentifier(Optional.of(record.getIdentifier()));
    calc.setTeamId(record.getTeamId());
    calc.setPropertyId(record.getPropertyId());
    calc.setContractId(Optional.ofNullable(record.getContractId()));
    calc.setSystemVersion(record.getSystemVersion());
    calc.setTotalPoints(record.getTotalPoints());
    calc.setSectorClassification(record.getSectorClassification());
    calc.setMaxRentIndication(Optional.ofNullable(record.getMaxRentIndication()));
    calc.setCategoryBreakdown(breakdown);
    calc.setInputDataJson(record.getInputData().data());
    calc.setCalculationDate(record.getCalculationDate());
    calc.setNotes(Optional.ofNullable(record.getNotes()));
    calc.setCreatedAt(record.getCreatedAt().toInstant(UTC));
    calc.setUpdatedAt(record.getUpdatedAt().toInstant(UTC));
    calc.setCreatedBy(record.getCreatedBy());
    calc.setUpdatedBy(record.getUpdatedBy());
    calc.setDeletedAt(Optional.ofNullable(record.getDeletedAt()).map(dt -> dt.toInstant(UTC)));

    return Optional.of(calc);
  }
}
