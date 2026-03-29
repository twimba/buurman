package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record AlContractMetadata(
    @Nullable String kontrataTip,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount garanciaAmount,
    @Nullable Integer garanciaMonths,
    @Nullable Boolean tatimoreRegistered,
    @Nullable MoneyAmount shpenzimet)
    implements ContractCountryMetadata {}
