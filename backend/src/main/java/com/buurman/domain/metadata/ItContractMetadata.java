package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record ItContractMetadata(
    @Nullable String contractCategory,
    @Nullable Boolean cedolareSecca,
    @Nullable BigDecimal cedolareRate,
    @Nullable String registrationNumber,
    @Nullable String apeRating,
    @Nullable MoneyAmount depositoAmount,
    @Nullable Integer depositoMonths)
    implements ContractCountryMetadata {}
