package com.buurman.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyInsurance;

import jakarta.validation.constraints.Positive;

public record UpdatePropertyInsuranceRequest(
    Optional<PropertyInsurance.InsuranceType> insuranceType,
    Optional<String> provider,
    Optional<String> policyNumber,
    Optional<@Positive(message = "Coverage amount must be positive") BigDecimal> coverageAmount,
    Optional<String> coverageAmountCurrency,
    Optional<@Positive(message = "Annual premium must be positive") BigDecimal> annualPremium,
    Optional<String> annualPremiumCurrency,
    Optional<String> paymentFrequency,
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate,
    Optional<PropertyInsurance.InsuranceStatus> status,
    Optional<String> notes) {}
