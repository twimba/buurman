package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record PropertyTimelineResponse(
    Optional<LocalDate> acquisitionDate,
    List<TimelineEntry> entries,
    List<FinancingEntry> financings) {

  public record TimelineEntry(
      TimelineEntryType type,
      String identifier,
      LocalDate startDate,
      Optional<LocalDate> endDate,
      Optional<String> description,
      Optional<String> metadata) {}

  public record FinancingEntry(
      String identifier,
      LocalDate startDate,
      Optional<LocalDate> endDate,
      String financingType,
      String status,
      Optional<String> lenderName,
      BigDecimal originalAmount,
      String originalAmountCurrency,
      Optional<BigDecimal> interestRate) {}

  public enum TimelineEntryType {
    SELF_OCCUPANCY,
    CONTRACT,
    VACANCY
  }
}
