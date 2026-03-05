package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record LtContractMetadata(
    @Nullable String nuomosSutartisType,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount uzstatasAmount,
    @Nullable Integer uzstatasMonths,
    @Nullable Boolean registruCentras,
    @Nullable MoneyAmount komunaliniaiAmount)
    implements ContractCountryMetadata {}
