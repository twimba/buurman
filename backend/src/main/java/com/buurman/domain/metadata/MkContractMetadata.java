package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record MkContractMetadata(
    @Nullable String dogovorType,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal depozitAmount,
    @Nullable Integer depozitMonths,
    @Nullable Boolean ujpRegistered,
    @Nullable BigDecimal rezhiskiTroskoviAmount)
    implements ContractCountryMetadata {}
