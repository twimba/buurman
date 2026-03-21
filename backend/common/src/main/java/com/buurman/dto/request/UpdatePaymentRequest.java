package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.Payment.PaymentStatus;

import jakarta.validation.constraints.Positive;
import com.buurman.util.Generated;

@Generated
public record UpdatePaymentRequest(
    Optional<@Positive(message = "Amount must be positive") BigDecimal> amount,
    Optional<String> currency,
    Optional<LocalDate> dueDate,
    Optional<PaymentStatus> status,
    Optional<String> notes) {}
