package com.buurman.dto.response;

import java.time.Instant;

public record UserTeamResponse(
    String identifier,
    String teamName,
    String role,
    boolean isOwner,
    boolean isDefault,
    boolean isActive,
    int memberCount,
    Instant joinedAt) {}
