package com.buurman.dto.response.backoffice;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record BackofficeTeamResponse(
    String identifier,
    String teamName,
    long memberCount,
    @Nullable String ownerEmail,
    Instant createdAt,
    @Nullable Instant updatedAt) {}
