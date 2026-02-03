package com.buurman.dto.response;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
    UUID invitationId,
    String token,
    String email,
    UUID teamId,
    String teamName,
    String role,
    String inviterName,
    Instant expiresAt,
    String invitationUrl,
    boolean isExpired,
    boolean isAccepted
) {}
