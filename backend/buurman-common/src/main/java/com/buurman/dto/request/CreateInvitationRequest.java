package com.buurman.dto.request;

import com.buurman.domain.TeamRole;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@SkipTestCoverage
public record CreateInvitationRequest(@NotBlank @Email String email, @NotNull TeamRole role) {}
