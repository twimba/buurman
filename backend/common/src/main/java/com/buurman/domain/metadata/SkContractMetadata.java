package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record SkContractMetadata(
    @Nullable String najomnaZmluvaType,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount kauciaAmount,
    @Nullable Integer kauciaMonths,
    @Nullable MoneyAmount poplatkyAmount,
    @Nullable Boolean regulatedRent)
    implements ContractCountryMetadata {}
