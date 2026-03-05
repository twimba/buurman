package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record LvContractMetadata(
    @Nullable String iresLigumsType,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount drosibaNaudaAmount,
    @Nullable Integer drosibaNaudaMonths,
    @Nullable Boolean zemesgramataRegistered,
    @Nullable MoneyAmount komunalieAmount)
    implements ContractCountryMetadata {}
