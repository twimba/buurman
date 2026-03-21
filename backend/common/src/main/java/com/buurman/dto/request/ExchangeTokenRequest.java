package com.buurman.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import com.buurman.util.Generated;

@Generated
public record ExchangeTokenRequest(
    @NotNull(message = "Session token is required") UUID sessionToken) {}
