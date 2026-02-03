package com.buurman.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseBreakdownResponse(
        List<Category> categories,
        BigDecimal total,
        String currency
) {
    public record Category(
            String name,
            BigDecimal value,
            String color
    ) {}
}
