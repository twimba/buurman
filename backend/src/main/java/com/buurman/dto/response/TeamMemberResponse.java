package com.buurman.dto.response;

import java.time.Instant;

public record TeamMemberResponse(
    String userIdentifier,
    String email,
    String name,
    String role,
    boolean isOwner,
    Instant joinedAt,
    boolean isCurrentUser) {}
