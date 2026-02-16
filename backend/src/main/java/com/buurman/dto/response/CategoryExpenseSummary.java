package com.buurman.dto.response;

import java.math.BigDecimal;

public record CategoryExpenseSummary(
    String category, BigDecimal total, int count, double percentage) {}
