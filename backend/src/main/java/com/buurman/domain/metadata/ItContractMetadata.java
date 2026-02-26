package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record ItContractMetadata(
    @Nullable String contractCategory,
    @Nullable Boolean cedolareSecca,
    @Nullable BigDecimal cedolareRate,
    @Nullable String registrationNumber,
    @Nullable String apeRating,
    @Nullable BigDecimal depositoAmount,
    @Nullable Integer depositoMonths)
    implements ContractCountryMetadata {}
