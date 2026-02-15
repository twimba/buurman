package com.buurman.dto.request.backoffice;

import jakarta.validation.constraints.NotBlank;

public record SendRegistrationInvitationRequest(
    @NotBlank String recipient,
    @NotBlank String channel
) {}
