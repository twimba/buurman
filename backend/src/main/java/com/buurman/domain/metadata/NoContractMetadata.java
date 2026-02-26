package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record NoContractMetadata(
    @Nullable String husleielovType,
    @Nullable String energyLabel,
    @Nullable BigDecimal depositumskontoAmount,
    @Nullable Integer depositumskontoMonths,
    @Nullable Boolean husleietvistnemnda,
    @Nullable Boolean kommunalBolig)
    implements ContractCountryMetadata {}
