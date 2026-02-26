package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record ClContractMetadata(
    @Nullable String tipoArriendo,
    @Nullable BigDecimal garantiaAmount,
    @Nullable Integer garantiaMonths,
    @Nullable Boolean siiRegistered,
    @Nullable BigDecimal gastosComunes,
    @Nullable String rolPropiedad,
    @Nullable Boolean reajusteIpcApplicable)
    implements ContractCountryMetadata {}
