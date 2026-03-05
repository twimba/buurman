package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record PeContractMetadata(
    @Nullable String tipoContrato,
    @Nullable MoneyAmount garantiaAmount,
    @Nullable Integer garantiaMonths,
    @Nullable Boolean sunarpRegistered,
    @Nullable MoneyAmount mantenimientoAmount,
    @Nullable String partidaRegistral)
    implements ContractCountryMetadata {}
