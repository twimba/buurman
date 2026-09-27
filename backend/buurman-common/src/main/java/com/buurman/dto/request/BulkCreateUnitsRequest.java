package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.UnitType;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record BulkCreateUnitsRequest(
    @Min(value = 1, message = "Count must be at least 1") @Max(value = 200, message = "Count must be at most 200") int count,
    @NotNull(message = "Numbering pattern is required") NumberingPattern numberingPattern,
    @NotNull(message = "Unit type is required") UnitType unitType,
    Optional<Integer> startFloor) {

  /** How generated unit numbers are formatted. */
  public enum NumberingPattern {
    NUMERIC,
    ALPHABETIC,
    FLOOR_DOT_INDEX
  }
}
