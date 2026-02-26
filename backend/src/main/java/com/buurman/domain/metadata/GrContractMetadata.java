package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record GrContractMetadata(
    @Nullable String misthosisType,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal eggysisAmount,
    @Nullable Integer eggysisMonths,
    @Nullable Boolean enoikiostasiProtected,
    @Nullable BigDecimal koinochristaAmount,
    @Nullable String taxisRegistrationNumber)
    implements ContractCountryMetadata {}
