package com.buurman.dto.response;

import java.util.Optional;

import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record UserProfileResponse(
    Sid identifier,
    String email,
    String firstName,
    String lastName,
    Optional<String> phone,
    boolean phoneVerified) {}
