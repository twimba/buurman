package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record MxContractMetadata(
    @Nullable String estadoCode,
    @Nullable String contratoType,
    @Nullable MoneyAmount depositoAmount,
    @Nullable Integer depositoMonths,
    @Nullable Boolean profecoRegistered,
    @Nullable MoneyAmount mantenimientoAmount)
    implements ContractCountryMetadata {}
