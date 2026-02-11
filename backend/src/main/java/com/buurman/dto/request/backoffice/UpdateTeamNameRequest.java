package com.buurman.dto.request.backoffice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTeamNameRequest(
    @NotBlank @Size(min = 1, max = 100) String name
) {}
