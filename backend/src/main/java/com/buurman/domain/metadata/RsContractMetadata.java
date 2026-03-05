package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record RsContractMetadata(
    @Nullable String ugovorType,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount depozitAmount,
    @Nullable Integer depozitMonths,
    @Nullable Boolean poreskaUpravaRegistered,
    @Nullable MoneyAmount komunalniTroskoviAmount)
    implements ContractCountryMetadata {}
