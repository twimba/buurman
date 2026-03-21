package com.buurman.dto.request;

import java.util.UUID;

import com.buurman.util.Generated;

import jakarta.validation.constraints.NotNull;

@Generated
public record ExchangeTokenRequest(
    @NotNull(message = "Session token is required") UUID sessionToken) {}
