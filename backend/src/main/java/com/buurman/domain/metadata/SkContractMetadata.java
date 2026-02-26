package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record SkContractMetadata(
    @Nullable String najomnaZmluvaType,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal kauciaAmount,
    @Nullable Integer kauciaMonths,
    @Nullable BigDecimal poplatkyAmount,
    @Nullable Boolean regulatedRent)
    implements ContractCountryMetadata {}
