package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record LvContractMetadata(
    @Nullable String iresLigumsType,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal drosibaNaudaAmount,
    @Nullable Integer drosibaNaudaMonths,
    @Nullable Boolean zemesgramataRegistered,
    @Nullable BigDecimal komunalieAmount)
    implements ContractCountryMetadata {}
