package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.domain.Ulid;

public record UserProfileResponse(
    Ulid identifier,
    String email,
    String firstName,
    String lastName,
    Optional<String> phone,
    boolean phoneVerified) {}
