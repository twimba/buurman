package com.buurman.dto.response;

import java.time.Instant;

import com.buurman.domain.CalendarFeed;

public record CalendarFeedResponse(
    String identifier,
    CalendarFeed.FeedType feedType,
    String contractIdentifier,
    String propertyIdentifier,
    String tenantIdentifier,
    String entityLabel,
    Boolean enabled,
    String feedUrl,
    Instant createdAt,
    Instant updatedAt) {}
