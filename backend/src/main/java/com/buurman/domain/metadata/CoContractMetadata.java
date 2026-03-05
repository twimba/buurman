package com.buurman.domain.metadata;

import org.jspecify.annotations.Nullable;

import com.buurman.util.MoneyAmount;

public record CoContractMetadata(
    @Nullable String tipoContrato,
    @Nullable MoneyAmount depositoAmount,
    @Nullable Integer depositoMonths,
    @Nullable Boolean registraduriaInscribed,
    @Nullable MoneyAmount administracionAmount,
    @Nullable String matriculaInmobiliaria,
    @Nullable Boolean estratoApplicable)
    implements ContractCountryMetadata {}
