package com.buurman.dto.request.backoffice;

import java.util.Optional;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBuurmyRequest(
    @NotBlank @Email String email,
    @Size(min = 3, max = 50) Optional<String> username,
    @NotBlank @Size(max = 100) String firstName,
    @NotBlank @Size(max = 100) String lastName,
    @NotBlank @Size(min = 8, max = 128) String password,
    boolean temporaryPassword) {}
