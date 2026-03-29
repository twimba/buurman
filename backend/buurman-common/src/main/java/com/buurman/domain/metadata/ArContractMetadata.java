package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record ArContractMetadata(
    @Nullable String tipoContrato,
    @Nullable MoneyAmount depositoAmount,
    @Nullable Integer depositoMonths,
    @Nullable Boolean registroPropiedad,
    @Nullable MoneyAmount expensasAmount,
    @Nullable String contratoInscripcion,
    @Nullable Boolean actualizacionIpcApplicable)
    implements ContractCountryMetadata {}
