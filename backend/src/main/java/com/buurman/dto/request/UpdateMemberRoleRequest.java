package com.buurman.dto.request;

import com.buurman.domain.TeamRole;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(@NotNull TeamRole role) {}
