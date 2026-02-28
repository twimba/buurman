package com.buurman.dto.request;

import java.util.Optional;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to create a new tenant")
public record CreateTenantRequest(
    @NotBlank(message = "First name is required") String firstName,
    Optional<String> lastName,
    Optional<@Email(message = "Email must be valid") String> email,
    Optional<
            @Pattern(
                regexp = "^\\+[1-9]\\d{1,14}$",
                message = "Phone must be in E.164 format (e.g. +31612345678)")
            String>
        phone,
    Optional<String> taxNumber,
    Optional<String> idNumber,
    Optional<String> additionalInfo) {}
