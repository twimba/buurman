package com.buurman.dto.response.backoffice;

import java.time.Instant;

public record BackofficeTeamResponse(
    String identifier,
    String teamName,
    long memberCount,
    String ownerEmail,
    Instant createdAt,
    Instant updatedAt) {}
