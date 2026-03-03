package com.buurman.dto.response;

import java.time.Instant;

import com.buurman.domain.Ulid;

public record UserTeamResponse(
    Ulid identifier,
    String teamName,
    String role,
    boolean isOwner,
    boolean isDefault,
    boolean isActive,
    int memberCount,
    Instant joinedAt) {}
