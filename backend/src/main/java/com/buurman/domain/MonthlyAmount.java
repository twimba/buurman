package com.buurman.domain;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/** Month label (YYYY-MM) + total amount projection for trend queries. */
public record MonthlyAmount(String month, @Nullable BigDecimal amount) {}
