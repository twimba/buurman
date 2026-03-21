package com.buurman.dto.request;

import com.buurman.domain.TeamRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.buurman.util.Generated;

@Generated
public record CreateInvitationRequest(@NotBlank @Email String email, @NotNull TeamRole role) {}
