package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record RoContractMetadata(
    @Nullable String contractType,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount garantieAmount,
    @Nullable Integer garantieMonths,
    @Nullable Boolean anafRegistered,
    @Nullable MoneyAmount intretinereAmount)
    implements ContractCountryMetadata {}
