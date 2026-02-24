package com.buurman.domain;

import java.math.BigDecimal;
import java.util.Optional;

/** Month label (YYYY-MM) + total amount projection for trend queries. */
public record MonthlyAmount(String month, Optional<BigDecimal> amount) {}
