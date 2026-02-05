package com.buurman.dto.response;

import com.buurman.domain.CalendarFeed;

import java.time.Instant;

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
        Instant updatedAt
) {
}
