package com.buurman.dto.request;

import com.buurman.domain.TeamRole;
import com.buurman.util.Generated;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Generated
public record CreateInvitationRequest(@NotBlank @Email String email, @NotNull TeamRole role) {}
