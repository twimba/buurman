package com.buurman.dto.response;

/** Internal result from adding a rent period, wrapping the API response + adjustment info. */
public record AddRentPeriodResult(
    RentPeriodResponse rentPeriodResponse, int adjustmentPaymentsCreated) {}
