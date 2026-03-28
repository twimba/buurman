package com.buurman.dto.request;

import com.buurman.domain.TeamRole;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotNull;

@SkipTestCoverage
public record UpdateMemberRoleRequest(@NotNull TeamRole role) {}
