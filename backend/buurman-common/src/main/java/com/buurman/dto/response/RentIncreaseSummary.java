package com.buurman.dto.response;

import com.buurman.util.SkipTestCoverage;

@SkipTestCoverage
public record RentIncreaseSummary(
    int totalContractsUpdated,
    int totalRentPeriodsCreated,
    int totalPaymentsCancelled,
    int totalPaymentsGenerated,
    int totalFailed) {}
