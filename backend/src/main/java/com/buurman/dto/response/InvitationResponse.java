package com.buurman.dto.response;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Ulid;

public record InvitationResponse(
    String token,
    String email,
    Ulid teamIdentifier,
    String teamName,
    String role,
    Optional<String> inviterName,
    Instant invitedAt,
    Optional<Instant> expiresAt,
    String invitationUrl,
    boolean isExpired,
    boolean isAccepted) {}
