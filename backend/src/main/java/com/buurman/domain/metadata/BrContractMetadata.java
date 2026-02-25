package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record BrContractMetadata(
    @Nullable String tipoLocacao,
    @Nullable BigDecimal caucaoAmount,
    @Nullable Integer caucaoMonths,
    @Nullable Boolean iptuIncluded,
    @Nullable BigDecimal condominioAmount,
    @Nullable String registroImobiliario,
    @Nullable Boolean seguroFianca)
    implements ContractCountryMetadata {}
