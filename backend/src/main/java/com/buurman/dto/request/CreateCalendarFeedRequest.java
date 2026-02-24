package com.buurman.dto.request;

import java.util.Objects;
import java.util.Optional;

import com.buurman.domain.CalendarFeed;

import jakarta.validation.constraints.NotNull;

public record CreateCalendarFeedRequest(
    @NotNull CalendarFeed.FeedType feedType,
    Optional<String> contractIdentifier,
    Optional<String> propertyIdentifier,
    Optional<String> tenantIdentifier) {
  public CreateCalendarFeedRequest {
    contractIdentifier = Objects.requireNonNullElse(contractIdentifier, Optional.empty());
    propertyIdentifier = Objects.requireNonNullElse(propertyIdentifier, Optional.empty());
    tenantIdentifier = Objects.requireNonNullElse(tenantIdentifier, Optional.empty());
  }
}
