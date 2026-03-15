package com.buurman.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ExchangeTokenRequest(
    @NotNull(message = "Session token is required") UUID sessionToken) {}
