package com.buurman.dto.response;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

public record DataDateRangeResponse(Optional<LocalDate> earliestDate) {
  public DataDateRangeResponse {
    earliestDate = Objects.requireNonNullElse(earliestDate, Optional.empty());
  }
}
