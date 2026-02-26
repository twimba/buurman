package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record PeContractMetadata(
    @Nullable String tipoContrato,
    @Nullable BigDecimal garantiaAmount,
    @Nullable Integer garantiaMonths,
    @Nullable Boolean sunarpRegistered,
    @Nullable BigDecimal mantenimientoAmount,
    @Nullable String partidaRegistral)
    implements ContractCountryMetadata {}
