package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record HuContractMetadata(
    @Nullable String berletiszerzodesType,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount kaucioAmount,
    @Nullable Integer kaucioMonths,
    @Nullable MoneyAmount kozosKoltsegAmount,
    @Nullable Boolean lakberApplicable)
    implements ContractCountryMetadata {}
