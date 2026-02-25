package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record BgContractMetadata(
    @Nullable String contractType,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal depozitAmount,
    @Nullable Integer depozitMonths,
    @Nullable Boolean notarizedContract,
    @Nullable BigDecimal obshtiRazhodiAmount)
    implements ContractCountryMetadata {}
