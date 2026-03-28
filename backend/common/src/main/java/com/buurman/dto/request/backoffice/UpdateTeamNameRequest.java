package com.buurman.dto.request.backoffice;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@SkipTestCoverage
public record UpdateTeamNameRequest(@NotBlank @Size(min = 1, max = 100) String name) {}
