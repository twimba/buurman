package com.buurman.dto.response;

import java.time.Instant;
import java.util.UUID;

public record InvitationResponse(
    UUID invitationId,
    String token,
    String email,
    UUID teamId,
    String role,
    Instant expiresAt,
    String invitationUrl
) {}
