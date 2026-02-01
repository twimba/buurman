package com.buurman.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateMemberRoleRequest(
    @NotBlank @Pattern(regexp = "TEAM_ADMIN|TEAM_EDITOR|TEAM_VIEWER") String role
) {}
