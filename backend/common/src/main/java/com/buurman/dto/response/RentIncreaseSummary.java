package com.buurman.dto.response;

public record RentIncreaseSummary(
    int totalContractsUpdated,
    int totalRentPeriodsCreated,
    int totalPaymentsCancelled,
    int totalPaymentsGenerated,
    int totalFailed) {}
