package com.buurman.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateTenantRequest(
    @NotBlank(message = "First name is required") String firstName,
    String lastName,
    @Email(message = "Email must be valid") String email,
    @Pattern(
            regexp = "^\\+[1-9]\\d{1,14}$",
            message = "Phone must be in E.164 format (e.g. +31612345678)")
        String phone,
    String taxNumber,
    String idNumber,
    String additionalInfo) {}
