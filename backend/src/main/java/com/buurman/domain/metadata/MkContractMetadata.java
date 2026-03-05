package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record MkContractMetadata(
    @Nullable String dogovorType,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount depozitAmount,
    @Nullable Integer depozitMonths,
    @Nullable Boolean ujpRegistered,
    @Nullable MoneyAmount rezhiskiTroskoviAmount)
    implements ContractCountryMetadata {}
