package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record EeContractMetadata(
    @Nullable String uuerilepinguType,
    @Nullable String energyCertificateRating,
    @Nullable MoneyAmount tagatisrahaAmount,
    @Nullable Integer tagatisrahaMonths,
    @Nullable Boolean kinnistusraamatRegistered,
    @Nullable MoneyAmount kommunaalkuludAmount)
    implements ContractCountryMetadata {}
