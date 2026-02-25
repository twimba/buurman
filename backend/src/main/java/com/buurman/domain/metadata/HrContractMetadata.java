package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record HrContractMetadata(
    @Nullable String ugovorType,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal jamcevinaAmount,
    @Nullable Integer jamcevinaMonths,
    @Nullable Boolean poreznaUprava,
    @Nullable BigDecimal pricuvaAmount)
    implements ContractCountryMetadata {}
