package com.buurman.dto.request.backoffice;

import com.buurman.util.Generated;

import jakarta.validation.constraints.NotBlank;

@Generated
public record SendRegistrationInvitationRequest(
    @NotBlank String recipient, @NotBlank String channel) {}
