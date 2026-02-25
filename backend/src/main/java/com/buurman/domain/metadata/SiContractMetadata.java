package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record SiContractMetadata(
    @Nullable String najemnaPogodbaTip,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal varscinsAmount,
    @Nullable Integer varscinsMonths,
    @Nullable Boolean neprofitnoStanovanje,
    @Nullable BigDecimal rezervniFondAmount)
    implements ContractCountryMetadata {}
