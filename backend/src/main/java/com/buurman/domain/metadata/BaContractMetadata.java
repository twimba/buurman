package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record BaContractMetadata(
    @Nullable String ugovorType,
    @Nullable String entityRegion,
    @Nullable BigDecimal depozitAmount,
    @Nullable Integer depozitMonths,
    @Nullable Boolean poreskaUpravaRegistered,
    @Nullable BigDecimal rezijeAmount)
    implements ContractCountryMetadata {}
