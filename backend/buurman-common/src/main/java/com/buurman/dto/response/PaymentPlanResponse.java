package com.buurman.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.buurman.domain.PaymentPlan;
import com.buurman.domain.Sid;
import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record PaymentPlanResponse(
    Sid identifier,
    Sid contractIdentifier,
    BigDecimal totalAmount,
    String currency,
    int instalmentCount,
    LocalDate startDate,
    PaymentPlan.Frequency frequency,
    PaymentPlan.PlanStatus status,
    Optional<String> notes,
    Optional<String> cancelReason,
    BigDecimal paidAmount,
    BigDecimal remainingAmount,
    List<PaymentSummary> instalments,
    List<Sid> coveredPaymentIdentifiers,
    Instant createdAt) {}
