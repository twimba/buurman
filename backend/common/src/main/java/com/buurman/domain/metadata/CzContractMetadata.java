package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record CzContractMetadata(
    @Nullable String najemniSmlouvaType,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount kauceAmount,
    @Nullable Integer kauceMonths,
    @Nullable MoneyAmount sluzbyAmount,
    @Nullable Boolean regulatedRent)
    implements ContractCountryMetadata {}
