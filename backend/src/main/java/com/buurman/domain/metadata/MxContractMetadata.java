package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record MxContractMetadata(
    @Nullable String estadoCode,
    @Nullable String contratoType,
    @Nullable BigDecimal depositoAmount,
    @Nullable Integer depositoMonths,
    @Nullable Boolean profecoRegistered,
    @Nullable BigDecimal mantenimientoAmount)
    implements ContractCountryMetadata {}
