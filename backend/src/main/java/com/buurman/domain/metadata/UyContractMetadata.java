package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record UyContractMetadata(
    @Nullable String garantiaType,
    @Nullable String tipoContrato,
    @Nullable MoneyAmount depositoAmount,
    @Nullable Integer depositoMonths,
    @Nullable Boolean dgiRegistered,
    @Nullable MoneyAmount gastosComunes,
    @Nullable String padronNumero)
    implements ContractCountryMetadata {}
