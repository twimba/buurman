package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record NoContractMetadata(
    @Nullable String husleielovType,
    @Nullable String energyLabel,
    @Nullable MoneyAmount depositumskontoAmount,
    @Nullable Integer depositumskontoMonths,
    @Nullable Boolean husleietvistnemnda,
    @Nullable Boolean kommunalBolig)
    implements ContractCountryMetadata {}
