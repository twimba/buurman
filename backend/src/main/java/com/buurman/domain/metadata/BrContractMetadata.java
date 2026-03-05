package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record BrContractMetadata(
    @Nullable String tipoLocacao,
    @Nullable MoneyAmount caucaoAmount,
    @Nullable Integer caucaoMonths,
    @Nullable Boolean iptuIncluded,
    @Nullable MoneyAmount condominioAmount,
    @Nullable String registroImobiliario,
    @Nullable Boolean seguroFianca)
    implements ContractCountryMetadata {}
