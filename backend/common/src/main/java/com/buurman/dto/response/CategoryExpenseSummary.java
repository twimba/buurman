package com.buurman.dto.response;

import java.math.BigDecimal;

import com.buurman.util.Generated;

@Generated
public record CategoryExpenseSummary(
    String category, BigDecimal total, int count, double percentage) {}
