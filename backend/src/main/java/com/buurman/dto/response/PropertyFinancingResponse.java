package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyFinancing;

public record PropertyFinancingResponse(
    String identifier,
    String propertyIdentifier,
    PropertyFinancing.FinancingType financingType,
    PropertyFinancing.RateType rateType,
    Optional<String> lenderName,
    Optional<String> loanNumber,
    BigDecimal originalAmount,
    String originalAmountCurrency,
    Optional<BigDecimal> currentBalance,
    Optional<String> currentBalanceCurrency,
    Optional<BigDecimal> interestRate,
    Optional<BigDecimal> monthlyPayment,
    Optional<String> monthlyPaymentCurrency,
    boolean paymentVariable,
    LocalDate startDate,
    Optional<LocalDate> endDate,
    Optional<Integer> termMonths,
    PropertyFinancing.FinancingStatus status,
    Optional<String> notes,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
