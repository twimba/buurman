package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record BgContractMetadata(
    @Nullable String contractType,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount depozitAmount,
    @Nullable Integer depozitMonths,
    @Nullable Boolean notarizedContract,
    @Nullable MoneyAmount obshtiRazhodiAmount)
    implements ContractCountryMetadata {}
