package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record AtContractMetadata(
    @Nullable String mietrechtsgesetzCategory,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal betriebskostenAmount,
    @Nullable Integer kautionMonths,
    @Nullable BigDecimal kautionAmount,
    @Nullable Boolean befristung,
    @Nullable BigDecimal richtwertmiete,
    @Nullable String energyCertificateNumber)
    implements ContractCountryMetadata {}
