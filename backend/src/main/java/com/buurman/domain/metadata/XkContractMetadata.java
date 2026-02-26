package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record XkContractMetadata(
    @Nullable String kontrataTip,
    @Nullable BigDecimal depozitAmount,
    @Nullable Integer depozitMonths,
    @Nullable Boolean tatRegistered,
    @Nullable BigDecimal shpenzimetKomunale,
    @Nullable String komunaRegistration)
    implements ContractCountryMetadata {}
