package com.buurman.dto.response;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public record PropertyTimelineResponse(List<TimelineEntry> entries) {

  public record TimelineEntry(
      TimelineEntryType type,
      String identifier,
      LocalDate startDate,
      Optional<LocalDate> endDate,
      Optional<String> description,
      Optional<String> metadata) {}

  public enum TimelineEntryType {
    SELF_OCCUPANCY,
    CONTRACT,
    VACANCY
  }
}
