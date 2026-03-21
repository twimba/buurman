package com.buurman.dto.request;

import com.buurman.util.Generated;

import jakarta.validation.constraints.NotBlank;

@Generated
public record UpdateProfileRequest(@NotBlank String firstName, @NotBlank String lastName) {}
