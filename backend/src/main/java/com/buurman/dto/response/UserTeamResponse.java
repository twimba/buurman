package com.buurman.dto.response;

import java.time.Instant;
import java.util.UUID;

public record UserTeamResponse(
    UUID teamId,
    String teamName,
    String identifier,
    String role,
    boolean isOwner,
    boolean isDefault,
    boolean isActive,
    int memberCount,
    Instant joinedAt
) {}
