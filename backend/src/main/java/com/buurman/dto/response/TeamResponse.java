package com.buurman.dto.response;

import java.time.Instant;

public record TeamResponse(
    String identifier,
    String teamName,
    long memberCount,
    Instant createdAt
) {}
