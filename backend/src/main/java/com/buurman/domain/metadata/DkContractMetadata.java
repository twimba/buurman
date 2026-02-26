package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record DkContractMetadata(
    @Nullable String lejelovType,
    @Nullable String energyLabel,
    @Nullable BigDecimal depositumAmount,
    @Nullable Integer depositumMonths,
    @Nullable BigDecimal forudbetalingAmount,
    @Nullable Integer forudbetalingMonths,
    @Nullable Boolean huslejenaevnEligible)
    implements ContractCountryMetadata {}
