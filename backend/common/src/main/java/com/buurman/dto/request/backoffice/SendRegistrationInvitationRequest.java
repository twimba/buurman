package com.buurman.dto.request.backoffice;

import jakarta.validation.constraints.NotBlank;
import com.buurman.util.Generated;

@Generated
public record SendRegistrationInvitationRequest(
    @NotBlank String recipient, @NotBlank String channel) {}
