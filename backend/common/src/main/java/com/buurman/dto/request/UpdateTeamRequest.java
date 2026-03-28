package com.buurman.dto.request;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record UpdateTeamRequest(
    @NotBlank(message = "Team name is required") @Size(min = 2, max = 100, message = "Team name must be between 2 and 100 characters") String name) {}
