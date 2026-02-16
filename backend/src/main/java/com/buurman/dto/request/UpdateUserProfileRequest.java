package com.buurman.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
    @NotBlank(message = "First name is required") @Size(min = 1, max = 100, message = "First name must be between 1 and 100 characters") String firstName,
    @NotBlank(message = "Last name is required") @Size(min = 1, max = 100, message = "Last name must be between 1 and 100 characters") String lastName,
    @Pattern(
            regexp = "^\\+[1-9]\\d{1,14}$",
            message = "Phone must be in E.164 format (e.g. +31612345678)")
        String phone) {}
