package com.buurman.dto.request;

import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateTenantRequest(
    @NotBlank(message = "First name is required") String firstName,
    @Nullable String lastName,
    @Nullable @Email(message = "Email must be valid") String email,
    @Nullable @Pattern(
            regexp = "^\\+[1-9]\\d{1,14}$",
            message = "Phone must be in E.164 format (e.g. +31612345678)")
        String phone,
    @Nullable String taxNumber,
    @Nullable String idNumber,
    @Nullable String additionalInfo) {}
