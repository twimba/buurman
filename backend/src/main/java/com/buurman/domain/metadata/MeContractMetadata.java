package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record MeContractMetadata(
    @Nullable String ugovorType,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal depozitAmount,
    @Nullable Integer depozitMonths,
    @Nullable Boolean poreskaUpravaRegistered,
    @Nullable BigDecimal komunalijeAmount)
    implements ContractCountryMetadata {}
