package com.buurman.domain.metadata;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

public record EeContractMetadata(
    @Nullable String uuerilepinguType,
    @Nullable String energyCertificateRating,
    @Nullable BigDecimal tagatisrahaAmount,
    @Nullable Integer tagatisrahaMonths,
    @Nullable Boolean kinnistusraamatRegistered,
    @Nullable BigDecimal kommunaalkuludAmount)
    implements ContractCountryMetadata {}
