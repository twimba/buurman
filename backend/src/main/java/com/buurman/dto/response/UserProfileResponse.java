package com.buurman.dto.response;

import java.util.UUID;

public record UserProfileResponse(
    UUID userId,
    String email,
    String firstName,
    String lastName
) {}
