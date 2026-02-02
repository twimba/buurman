package com.buurman.dto.response;

import java.math.BigDecimal;

public record DashboardStatsResponse(
        int totalProperties,
        int occupiedUnits,
        int vacantUnits,
        int maintenanceUnits,
        int unavailableUnits,
        MonthlyIncome monthlyIncome,
        BigDecimal occupancyRate
) {
    public record MonthlyIncome(
            BigDecimal amount,
            String currency
    ) {}
}
