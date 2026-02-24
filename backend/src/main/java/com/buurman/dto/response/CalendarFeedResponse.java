package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.CalendarFeed;

public record CalendarFeedResponse(
    String identifier,
    CalendarFeed.FeedType feedType,
    Optional<String> contractIdentifier,
    Optional<String> propertyIdentifier,
    Optional<String> tenantIdentifier,
    Optional<String> entityLabel,
    Boolean enabled,
    String feedUrl,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
