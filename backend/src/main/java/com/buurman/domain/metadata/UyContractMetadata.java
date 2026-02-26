package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record UyContractMetadata(
    @Nullable String garantiaType,
    @Nullable String tipoContrato,
    @Nullable BigDecimal depositoAmount,
    @Nullable Integer depositoMonths,
    @Nullable Boolean dgiRegistered,
    @Nullable BigDecimal gastosComunes,
    @Nullable String padronNumero)
    implements ContractCountryMetadata {}
