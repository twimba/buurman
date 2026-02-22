package com.buurman.dto.request;

import org.jspecify.annotations.Nullable;

import com.buurman.domain.CalendarFeed;

import jakarta.validation.constraints.NotNull;

public record CreateCalendarFeedRequest(
    @NotNull CalendarFeed.FeedType feedType,
    @Nullable String contractIdentifier,
    @Nullable String propertyIdentifier,
    @Nullable String tenantIdentifier) {}
