package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record UsContractMetadata(
    @Nullable String state,
    @Nullable Boolean rentControlled,
    @Nullable String rentControlJurisdiction,
    @Nullable Boolean section8Eligible,
    @Nullable Boolean leadPaintDisclosure,
    @Nullable MoneyAmount securityDepositLimit,
    @Nullable Integer securityDepositMonths)
    implements ContractCountryMetadata {}
