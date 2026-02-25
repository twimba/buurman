package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record AlContractMetadata(
    @Nullable String kontrataTip,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal garanciaAmount,
    @Nullable Integer garanciaMonths,
    @Nullable Boolean tatimoreRegistered,
    @Nullable BigDecimal shpenzimet)
    implements ContractCountryMetadata {}
