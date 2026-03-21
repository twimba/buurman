package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.util.Generated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Generated
public record CreatePaymentReceivalRequest(
    @NotNull(message = "Amount is required") @Positive(message = "Amount must be positive") BigDecimal amount,
    @NotNull(message = "Receival date is required") LocalDate receivalDate,
    Optional<String> notes) {}
