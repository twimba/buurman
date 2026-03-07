package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record AtContractMetadata(
    @Nullable String mietrechtsgesetzCategory,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount betriebskostenAmount,
    @Nullable Integer kautionMonths,
    @Nullable MoneyAmount kautionAmount,
    @Nullable Boolean befristung,
    @Nullable MoneyAmount richtwertmiete,
    @Nullable String energyCertificateNumber)
    implements ContractCountryMetadata {}
