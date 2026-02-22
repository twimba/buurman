package com.buurman.dto.response;

import org.jspecify.annotations.Nullable;

public record UserProfileResponse(
    String identifier,
    String email,
    String firstName,
    String lastName,
    @Nullable String phone,
    boolean phoneVerified) {}
