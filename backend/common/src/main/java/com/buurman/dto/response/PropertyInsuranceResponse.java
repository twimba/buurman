package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import com.buurman.domain.PropertyInsurance;
import com.buurman.domain.Sid;
import com.buurman.util.Generated;

@Generated
public record PropertyInsuranceResponse(
    Sid identifier,
    Optional<PropertySummary> property,
    PropertyInsurance.InsuranceType insuranceType,
    Optional<String> provider,
    Optional<String> policyNumber,
    Optional<BigDecimal> coverageAmount,
    Optional<String> coverageAmountCurrency,
    BigDecimal annualPremium,
    String annualPremiumCurrency,
    String paymentFrequency,
    Optional<LocalDate> startDate,
    Optional<LocalDate> endDate,
    PropertyInsurance.InsuranceStatus status,
    Optional<String> notes,
    Instant createdAt,
    Optional<Instant> updatedAt) {}
