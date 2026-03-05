package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record HrContractMetadata(
    @Nullable String ugovorType,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount jamcevinaAmount,
    @Nullable Integer jamcevinaMonths,
    @Nullable Boolean poreznaUprava,
    @Nullable MoneyAmount pricuvaAmount)
    implements ContractCountryMetadata {}
