package com.buurman.dto.response;

public record UserProfileResponse(
    String identifier,
    String email,
    String firstName,
    String lastName
) {}
