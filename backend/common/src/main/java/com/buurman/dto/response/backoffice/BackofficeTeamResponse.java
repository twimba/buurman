package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record BackofficeTeamResponse(
    Sid identifier,
    String teamName,
    boolean demo,
    long memberCount,
    Optional<String> ownerEmail,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
