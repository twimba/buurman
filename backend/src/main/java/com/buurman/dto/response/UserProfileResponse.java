package com.buurman.dto.response;

import java.util.Optional;

public record UserProfileResponse(
    String identifier,
    String email,
    String firstName,
    String lastName,
    Optional<String> phone,
    boolean phoneVerified) {}
