package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyFinancing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreatePropertyFinancingRequest(
    @NotNull(message = "Financing type is required") PropertyFinancing.FinancingType financingType,
    @NotNull(message = "Rate type is required") PropertyFinancing.RateType rateType,
    Optional<String> lenderName,
    Optional<String> loanNumber,
    @NotNull(message = "Original amount is required") @Positive(message = "Original amount must be positive") BigDecimal originalAmount,
    @NotBlank(message = "Original amount currency is required") String originalAmountCurrency,
    Optional<@PositiveOrZero(message = "Current balance must be zero or positive") BigDecimal>
        currentBalance,
    Optional<String> currentBalanceCurrency,
    Optional<@PositiveOrZero(message = "Interest rate must be zero or positive") BigDecimal>
        interestRate,
    Optional<@Positive(message = "Monthly payment must be positive") BigDecimal> monthlyPayment,
    Optional<String> monthlyPaymentCurrency,
    Optional<Boolean> paymentVariable,
    @NotNull(message = "Start date is required") LocalDate startDate,
    Optional<LocalDate> endDate,
    Optional<@Positive(message = "Term months must be positive") Integer> termMonths,
    Optional<PropertyFinancing.FinancingStatus> status,
    Optional<String> notes) {}
