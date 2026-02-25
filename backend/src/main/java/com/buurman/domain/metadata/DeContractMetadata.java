package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record DeContractMetadata(
    @Nullable String mietspiegelReference,
    @Nullable Boolean mietpreisbremseApplicable,
    @Nullable String rentType,
    @Nullable Boolean warmRent,
    @Nullable BigDecimal nebenkostenAmount,
    @Nullable BigDecimal kautionAmount,
    @Nullable Integer kautionMonths,
    @Nullable String energyCertificateType,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal energyCertificateValue)
    implements ContractCountryMetadata {}
