package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record ArContractMetadata(
    @Nullable String tipoContrato,
    @Nullable BigDecimal depositoAmount,
    @Nullable Integer depositoMonths,
    @Nullable Boolean registroPropiedad,
    @Nullable BigDecimal expensasAmount,
    @Nullable String contratoInscripcion,
    @Nullable Boolean actualizacionIpcApplicable)
    implements ContractCountryMetadata {}
