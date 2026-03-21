package com.buurman.dto.request;

import java.math.BigDecimal;
import java.util.Optional;

import com.buurman.domain.RentComponentType;
import com.buurman.util.Generated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Generated
public record RentComponentRequest(
    @NotNull(message = "Component type is required") RentComponentType componentType,
    @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal amount,
    Optional<String> description) {}
