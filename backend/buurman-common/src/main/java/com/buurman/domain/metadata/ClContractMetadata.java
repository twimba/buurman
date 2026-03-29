package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record ClContractMetadata(
    @Nullable String tipoArriendo,
    @Nullable MoneyAmount garantiaAmount,
    @Nullable Integer garantiaMonths,
    @Nullable Boolean siiRegistered,
    @Nullable MoneyAmount gastosComunes,
    @Nullable String rolPropiedad,
    @Nullable Boolean reajusteIpcApplicable)
    implements ContractCountryMetadata {}
