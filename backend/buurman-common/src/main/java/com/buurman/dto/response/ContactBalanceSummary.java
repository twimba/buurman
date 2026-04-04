package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.Optional;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record ContactBalanceSummary(
    BigDecimal outstandingAmount,
    String currency,
    BalanceStatus status,
    int outstandingPaymentCount,
    Optional<BigDecimal> guaranteedAmount,
    Optional<Integer> guaranteedPaymentCount) {}
