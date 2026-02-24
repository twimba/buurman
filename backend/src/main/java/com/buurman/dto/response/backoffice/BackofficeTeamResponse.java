package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.Optional;

public record BackofficeTeamResponse(
    String identifier,
    String teamName,
    boolean demo,
    long memberCount,
    Optional<String> ownerEmail,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
