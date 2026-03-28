package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.Payment;
import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record PaymentResponse(
    Sid identifier,
    Optional<ContractSummary> contract,
    Optional<ContactSummary> contact,
    Optional<PropertySummary> property,
    BigDecimal amount,
    String currency,
    Optional<BigDecimal> receivedAmount,
    Optional<BigDecimal> balance,
    Optional<LocalDate> paymentDate,
    LocalDate dueDate,
    Payment.PaymentStatus status,
    Optional<String> notes,
    Optional<DocumentResponse> proofOfPayment,
    Optional<DocumentResponse> receipt,
    List<PaymentReceivalResponse> receivals,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
