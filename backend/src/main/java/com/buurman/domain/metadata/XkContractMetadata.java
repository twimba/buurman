package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record XkContractMetadata(
    @Nullable String kontrataTip,
    @Nullable MoneyAmount depozitAmount,
    @Nullable Integer depozitMonths,
    @Nullable Boolean tatRegistered,
    @Nullable MoneyAmount shpenzimetKomunale,
    @Nullable String komunaRegistration)
    implements ContractCountryMetadata {}
