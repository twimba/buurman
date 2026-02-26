package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record GenericContractMetadata(
    @Nullable String energyCertificateRating,
    @Nullable String energyCertificateNumber,
    @Nullable String contractRegistrationNumber,
    @Nullable BigDecimal maxDepositMonths,
    @Nullable Boolean rentIndexationApplicable,
    @Nullable String notes)
    implements ContractCountryMetadata {}
