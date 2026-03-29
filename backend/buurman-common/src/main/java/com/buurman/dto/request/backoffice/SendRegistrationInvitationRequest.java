package com.buurman.dto.request.backoffice;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;

@SkipTestCoverage
public record SendRegistrationInvitationRequest(
    @NotBlank String recipient, @NotBlank String channel) {}
