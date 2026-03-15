package com.buurman.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejoinImpersonationRequest(
    @NotBlank(message = "Password is required") String password) {}
