package com.buurman.dto.response;

import com.buurman.util.Generated;

@Generated
public record RentIncreaseSummary(
    int totalContractsUpdated,
    int totalRentPeriodsCreated,
    int totalPaymentsCancelled,
    int totalPaymentsGenerated,
    int totalFailed) {}
