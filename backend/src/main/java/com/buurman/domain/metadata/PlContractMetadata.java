package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record PlContractMetadata(
    @Nullable String rodzajNajmu,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal kaucjaAmount,
    @Nullable Integer kaucjaMonths,
    @Nullable Boolean indexationApplicable,
    @Nullable String energyCertificateNumber,
    @Nullable Boolean czynszdodatkowy)
    implements ContractCountryMetadata {}
