package com.buurman.domain;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/** Count + total amount projection for payment/expense aggregate queries. */
public record AmountStats(int count, @Nullable BigDecimal total) {}
