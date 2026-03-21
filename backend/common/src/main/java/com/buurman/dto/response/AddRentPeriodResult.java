package com.buurman.dto.response;

import com.buurman.util.Generated;

/** Internal result from adding a rent period, wrapping the API response + adjustment info. */
@Generated
public record AddRentPeriodResult(
    RentPeriodResponse rentPeriodResponse, int adjustmentPaymentsCreated) {}
