package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record BaContractMetadata(
    @Nullable String ugovorType,
    @Nullable String entityRegion,
    @Nullable MoneyAmount depozitAmount,
    @Nullable Integer depozitMonths,
    @Nullable Boolean poreskaUpravaRegistered,
    @Nullable MoneyAmount rezijeAmount)
    implements ContractCountryMetadata {}
