package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.CalendarFeed;
import com.buurman.domain.Ulid;

public record CalendarFeedResponse(
    Ulid identifier,
    CalendarFeed.FeedType feedType,
    Optional<Ulid> contractIdentifier,
    Optional<Ulid> propertyIdentifier,
    Optional<Ulid> tenantIdentifier,
    Optional<String> entityLabel,
    Boolean enabled,
    String feedUrl,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
