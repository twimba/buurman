package com.buurman.dto.request;

import jakarta.validation.constraints.NotBlank;
import com.buurman.util.Generated;

@Generated
public record UpdateProfileRequest(@NotBlank String firstName, @NotBlank String lastName) {}
