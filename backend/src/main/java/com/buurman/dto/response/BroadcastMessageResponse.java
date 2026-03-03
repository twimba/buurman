package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

public record BroadcastMessageResponse(
    String identifier,
    String title,
    String body,
    String severity,
    Instant startAt,
    Optional<Instant> endAt) {}
