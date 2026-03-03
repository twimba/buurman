package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.Optional;

public record BackofficeBroadcastMessageResponse(
    String identifier,
    String title,
    String body,
    String severity,
    Instant startAt,
    Optional<Instant> endAt,
    boolean showOnLogin,
    boolean showOnRegister,
    boolean showInApp,
    Instant createdAt,
    Instant updatedAt) {}
