package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.domain.TimelineEventType;

public record TimelineEventResponse(
    TimelineEventType type,
    Instant timestamp,
    String title,
    Optional<String> description,
    Optional<Sid> relatedIdentifier) {}
