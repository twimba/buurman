package com.buurman.dto.request;

import java.util.Optional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateTenantRequest(
    @NotBlank(message = "First name is required") String firstName,
    Optional<String> lastName,
    @Email(message = "Email must be valid") Optional<String> email,
    @Pattern(
            regexp = "^\\+[1-9]\\d{1,14}$",
            message = "Phone must be in E.164 format (e.g. +31612345678)")
        Optional<String> phone,
    Optional<String> taxNumber,
    Optional<String> idNumber,
    Optional<String> additionalInfo) {}
