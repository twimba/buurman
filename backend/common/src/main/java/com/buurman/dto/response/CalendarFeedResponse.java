package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.CalendarFeed;
import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record CalendarFeedResponse(
    Sid identifier,
    CalendarFeed.FeedType feedType,
    Optional<Sid> contractIdentifier,
    Optional<Sid> propertyIdentifier,
    Optional<Sid> tenantIdentifier,
    Optional<String> entityLabel,
    Boolean enabled,
    String feedUrl,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
