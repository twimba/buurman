package com.buurman.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TeamResponse(
    UUID teamId,
    String identifier,
    String teamName,
    long memberCount,
    Instant createdAt
) {}
