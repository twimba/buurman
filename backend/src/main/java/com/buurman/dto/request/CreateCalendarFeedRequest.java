package com.buurman.dto.request;

import com.buurman.domain.CalendarFeed;

import jakarta.validation.constraints.NotNull;

public record CreateCalendarFeedRequest(
    @NotNull CalendarFeed.FeedType feedType,
    String contractIdentifier,
    String propertyIdentifier,
    String tenantIdentifier) {}
