package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record GrContractMetadata(
    @Nullable String misthosisType,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount eggysisAmount,
    @Nullable Integer eggysisMonths,
    @Nullable Boolean enoikiostasiProtected,
    @Nullable MoneyAmount koinochristaAmount,
    @Nullable String taxisRegistrationNumber)
    implements ContractCountryMetadata {}
