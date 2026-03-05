package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

public record GenericContractMetadata(
    @Nullable String energyCertificateRating,
    @Nullable String energyCertificateNumber,
    @Nullable String contractRegistrationNumber,
    @Nullable Integer maxDepositMonths,
    @Nullable Boolean rentIndexationApplicable,
    @Nullable String notes)
    implements ContractCountryMetadata {}
