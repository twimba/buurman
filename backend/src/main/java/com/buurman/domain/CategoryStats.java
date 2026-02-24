package com.buurman.domain;

import java.math.BigDecimal;

import org.jspecify.annotations.Nullable;

/** Category + count + total projection for expense category breakdown queries. */
public record CategoryStats(String category, int count, @Nullable BigDecimal total) {}
