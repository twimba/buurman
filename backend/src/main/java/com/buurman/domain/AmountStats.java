package com.buurman.domain;

import java.math.BigDecimal;
import java.util.Optional;

/** Count + total amount projection for payment/expense aggregate queries. */
public record AmountStats(int count, Optional<BigDecimal> total) {}
