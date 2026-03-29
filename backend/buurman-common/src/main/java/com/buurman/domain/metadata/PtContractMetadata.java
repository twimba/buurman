package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record PtContractMetadata(
    @Nullable String nrauRegime,
    @Nullable Boolean oldLeaseRegime,
    @Nullable BigDecimal updateCoefficient,
    @Nullable String imiReference,
    @Nullable String energyCertificateRating)
    implements ContractCountryMetadata {}
