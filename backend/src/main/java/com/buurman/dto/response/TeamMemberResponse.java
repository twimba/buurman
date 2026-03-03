package com.buurman.dto.response;

import java.time.Instant;

import com.buurman.domain.Ulid;

public record TeamMemberResponse(
    Ulid userIdentifier,
    String email,
    String name,
    String role,
    boolean isOwner,
    Instant joinedAt,
    boolean isCurrentUser) {}
