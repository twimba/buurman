package com.buurman.domain;

import java.math.BigDecimal;

/** Projection of active contract income fields for monthly income calculation. */
public record ContractIncomeEntry(
    BigDecimal rentAmount, String rentAmountCurrency, String paymentFrequency) {}
