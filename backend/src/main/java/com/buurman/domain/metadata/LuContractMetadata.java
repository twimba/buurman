package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record LuContractMetadata(
    @Nullable String bailType,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount cautionAmount,
    @Nullable Integer cautionMonths,
    @Nullable Boolean loyerMaxApplicable,
    @Nullable MoneyAmount chargesAmount,
    @Nullable String registrationNumber)
    implements ContractCountryMetadata {}
