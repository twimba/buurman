package com.buurman.dto.request;

import com.buurman.domain.TeamRole;

import jakarta.validation.constraints.NotNull;
import com.buurman.util.Generated;

@Generated
public record UpdateMemberRoleRequest(@NotNull TeamRole role) {}
