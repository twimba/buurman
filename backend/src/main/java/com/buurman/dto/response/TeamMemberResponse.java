package com.buurman.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TeamMemberResponse(
    UUID memberId,
    UUID userId,
    String email,
    String name,
    String role,
    boolean isOwner,
    Instant joinedAt,
    boolean isCurrentUser
) {}
