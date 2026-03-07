package com.buurman.domain;

import java.math.BigDecimal;
import java.util.Optional;

/** Category + count + total projection for expense category breakdown queries. */
public record CategoryStats(String category, int count, Optional<BigDecimal> total) {}
