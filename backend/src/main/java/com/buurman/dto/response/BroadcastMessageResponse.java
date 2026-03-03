package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Ulid;

public record BroadcastMessageResponse(
    Ulid identifier,
    String title,
    String body,
    String severity,
    Instant startAt,
    Optional<Instant> endAt) {}
