package com.buurman.dto.request;

import com.buurman.util.Generated;

import jakarta.validation.constraints.NotBlank;

@Generated
public record RejoinImpersonationRequest(
    @NotBlank(message = "Password is required") String password) {}
