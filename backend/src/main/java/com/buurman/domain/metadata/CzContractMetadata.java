package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record CzContractMetadata(
    @Nullable String najemniSmlouvaType,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal kauceAmount,
    @Nullable Integer kauceMonths,
    @Nullable BigDecimal sluzbyAmount,
    @Nullable Boolean regulatedRent)
    implements ContractCountryMetadata {}
