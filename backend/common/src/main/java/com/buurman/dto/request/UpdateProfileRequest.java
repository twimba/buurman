package com.buurman.dto.request;

import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.NotBlank;

@SkipTestCoverage
public record UpdateProfileRequest(@NotBlank String firstName, @NotBlank String lastName) {}
