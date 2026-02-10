package com.buurman.dto.response;

import java.time.Instant;

public record InvitationResponse(
    String token,
    String email,
    String teamIdentifier,
    String teamName,
    String role,
    String inviterName,
    Instant invitedAt,
    Instant expiresAt,
    String invitationUrl,
    boolean isExpired,
    boolean isAccepted
) {}
