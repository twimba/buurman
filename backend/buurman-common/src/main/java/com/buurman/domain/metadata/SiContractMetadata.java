package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record SiContractMetadata(
    @Nullable String najemnaPogodbaTip,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount varscinsAmount,
    @Nullable Integer varscinsMonths,
    @Nullable Boolean neprofitnoStanovanje,
    @Nullable MoneyAmount rezervniFondAmount)
    implements ContractCountryMetadata {}
