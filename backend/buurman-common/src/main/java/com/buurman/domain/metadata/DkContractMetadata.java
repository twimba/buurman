package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record DkContractMetadata(
    @Nullable String lejelovType,
    @Nullable String energyLabel,
    @Nullable MoneyAmount depositumAmount,
    @Nullable Integer depositumMonths,
    @Nullable MoneyAmount forudbetalingAmount,
    @Nullable Integer forudbetalingMonths,
    @Nullable Boolean huslejenaevnEligible)
    implements ContractCountryMetadata {}
