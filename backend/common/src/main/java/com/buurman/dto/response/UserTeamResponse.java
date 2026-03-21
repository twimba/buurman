package com.buurman.dto.response;

import java.time.Instant;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record UserTeamResponse(
    Sid identifier,
    String teamName,
    String role,
    boolean isOwner,
    boolean isDefault,
    boolean isActive,
    int memberCount,
    Instant joinedAt) {}
