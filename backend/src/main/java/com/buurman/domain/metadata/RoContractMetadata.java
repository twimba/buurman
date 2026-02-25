package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record RoContractMetadata(
    @Nullable String contractType,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal garantieAmount,
    @Nullable Integer garantieMonths,
    @Nullable Boolean anafRegistered,
    @Nullable BigDecimal intretinereAmount)
    implements ContractCountryMetadata {}
