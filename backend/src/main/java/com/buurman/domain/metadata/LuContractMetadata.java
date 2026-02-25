package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record LuContractMetadata(
    @Nullable String bailType,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal cautionAmount,
    @Nullable Integer cautionMonths,
    @Nullable Boolean loyerMaxApplicable,
    @Nullable BigDecimal chargesAmount,
    @Nullable String registrationNumber)
    implements ContractCountryMetadata {}
