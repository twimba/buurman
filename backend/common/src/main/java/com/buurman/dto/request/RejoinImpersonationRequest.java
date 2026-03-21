package com.buurman.dto.request;

import jakarta.validation.constraints.NotBlank;
import com.buurman.util.Generated;

@Generated
public record RejoinImpersonationRequest(
    @NotBlank(message = "Password is required") String password) {}
