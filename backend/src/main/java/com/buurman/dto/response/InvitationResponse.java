package com.buurman.dto.response;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record InvitationResponse(
    String token,
    String email,
    String teamIdentifier,
    String teamName,
    String role,
    @Nullable String inviterName,
    Instant invitedAt,
    @Nullable Instant expiresAt,
    String invitationUrl,
    boolean isExpired,
    boolean isAccepted) {}
