package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record HuContractMetadata(
    @Nullable String berletiszerzodesType,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal kaucioAmount,
    @Nullable Integer kaucioMonths,
    @Nullable BigDecimal kozosKoltsegAmount,
    @Nullable Boolean lakberApplicable)
    implements ContractCountryMetadata {}
