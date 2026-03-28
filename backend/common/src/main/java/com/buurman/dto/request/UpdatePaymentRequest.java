package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.Payment.PaymentStatus;
import com.buurman.domain.identifier.ContactIdentifier;
import com.buurman.util.SkipTestCoverage;

import jakarta.validation.constraints.Positive;

@SkipTestCoverage
public record UpdatePaymentRequest(
    Optional<@Positive(message = "Amount must be positive") BigDecimal> amount,
    Optional<String> currency,
    Optional<LocalDate> dueDate,
    Optional<PaymentStatus> status,
    Optional<String> notes,
    Optional<ContactIdentifier> contactIdentifier) {}
