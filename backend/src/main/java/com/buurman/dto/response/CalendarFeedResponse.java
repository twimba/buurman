package com.buurman.dto.response;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.CalendarFeed;

public record CalendarFeedResponse(
    String identifier,
    CalendarFeed.FeedType feedType,
    @Nullable String contractIdentifier,
    @Nullable String propertyIdentifier,
    @Nullable String tenantIdentifier,
    @Nullable String entityLabel,
    Boolean enabled,
    String feedUrl,
    Instant createdAt,
    @Nullable Instant updatedAt) {}
