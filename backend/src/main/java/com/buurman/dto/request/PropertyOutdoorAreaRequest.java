package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

import jakarta.validation.constraints.NotBlank;

public record PropertyOutdoorAreaRequest(
    @NotBlank(message = "Type is required") String type,
    Optional<BigDecimal> areaValue,
    Optional<String> areaUnit) {

  public PropertyOutdoorAreaRequest {
    areaValue = Objects.requireNonNullElse(areaValue, Optional.empty());
    areaUnit = Objects.requireNonNullElse(areaUnit, Optional.empty());
  }
}
