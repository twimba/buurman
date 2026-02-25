package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record UsContractMetadata(
    @Nullable String state,
    @Nullable Boolean rentControlled,
    @Nullable String rentControlJurisdiction,
    @Nullable Boolean section8Eligible,
    @Nullable Boolean leadPaintDisclosure,
    @Nullable BigDecimal securityDepositLimit,
    @Nullable Integer securityDepositMonths)
    implements ContractCountryMetadata {}
