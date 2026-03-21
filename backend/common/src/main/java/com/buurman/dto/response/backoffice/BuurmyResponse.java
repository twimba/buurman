package com.buurman.dto.response.backoffice;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.buurman.util.Generated;

@Generated
public record BuurmyResponse(
    String id,
    String username,
    String email,
    Optional<String> firstName,
    Optional<String> lastName,
    boolean enabled,
    boolean emailVerified,
    Optional<Instant> createdAt,
    Optional<Instant> lastLogin,
    List<String> requiredActions) {}
