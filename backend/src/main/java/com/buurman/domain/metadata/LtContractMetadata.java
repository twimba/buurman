package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record LtContractMetadata(
    @Nullable String nuomosSutartisType,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal uzstatasAmount,
    @Nullable Integer uzstatasMonths,
    @Nullable Boolean registruCentras,
    @Nullable BigDecimal komunaliniaiAmount)
    implements ContractCountryMetadata {}
