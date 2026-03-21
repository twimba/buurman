package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyFinancing;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import com.buurman.util.Generated;

@Generated
public record UpdatePropertyFinancingRequest(
    Optional<PropertyFinancing.FinancingType> financingType,
    Optional<PropertyFinancing.RateType> rateType,
    Optional<String> lenderName,
    Optional<String> loanNumber,
    Optional<@Positive(message = "Original amount must be positive") BigDecimal> originalAmount,
    Optional<String> originalAmountCurrency,
    Optional<@PositiveOrZero(message = "Current balance must be zero or positive") BigDecimal>
        currentBalance,
    Optional<String> currentBalanceCurrency,
    Optional<@PositiveOrZero(message = "Interest rate must be zero or positive") BigDecimal>
        interestRate,
    Optional<@Positive(message = "Monthly payment must be positive") BigDecimal> monthlyPayment,
    Optional<String> monthlyPaymentCurrency,
    Optional<Boolean> paymentVariable,
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate,
    Optional<@Positive(message = "Term months must be positive") Integer> termMonths,
    Optional<PropertyFinancing.FinancingStatus> status,
    Optional<String> notes) {}
