package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record PlContractMetadata(
    @Nullable String rodzajNajmu,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount kaucjaAmount,
    @Nullable Integer kaucjaMonths,
    @Nullable Boolean indexationApplicable,
    @Nullable String energyCertificateNumber,
    @Nullable Boolean czynszdodatkowy)
    implements ContractCountryMetadata {}
