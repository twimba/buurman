package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyFinancing;
import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record PropertyFinancingResponse(
    Sid identifier,
    Sid propertyIdentifier,
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
