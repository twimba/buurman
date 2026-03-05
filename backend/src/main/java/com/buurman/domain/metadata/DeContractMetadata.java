package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record DeContractMetadata(
    @Nullable String mietspiegelReference,
    @Nullable Boolean mietpreisbremseApplicable,
    @Nullable String rentType,
    @Nullable Boolean warmRent,
    @Nullable MoneyAmount nebenkostenAmount,
    @Nullable MoneyAmount kautionAmount,
    @Nullable Integer kautionMonths,
    @Nullable String energyCertificateType,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal energyCertificateValue)
    implements ContractCountryMetadata {}
