package com.buurman.dto.request;

import java.util.Optional;

import com.buurman.domain.CalendarFeed;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotNull;

@SkipTestCoverage
public record CreateCalendarFeedRequest(
    @NotNull CalendarFeed.FeedType feedType,
    Optional<String> contractIdentifier,
    Optional<String> propertyIdentifier,
    Optional<String> contactIdentifier) {}
