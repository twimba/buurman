package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyInsurance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreatePropertyInsuranceRequest(
    @NotNull(message = "Insurance type is required") PropertyInsurance.InsuranceType insuranceType,
    Optional<String> provider,
    Optional<String> policyNumber,
    Optional<@Positive(message = "Coverage amount must be positive") BigDecimal> coverageAmount,
    Optional<String> coverageAmountCurrency,
    @NotNull(message = "Annual premium is required") @Positive(message = "Annual premium must be positive") BigDecimal annualPremium,
    @NotBlank(message = "Annual premium currency is required") String annualPremiumCurrency,
    @NotBlank(message = "Payment frequency is required") String paymentFrequency,
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate,
    Optional<PropertyInsurance.InsuranceStatus> status,
    Optional<String> notes) {}
