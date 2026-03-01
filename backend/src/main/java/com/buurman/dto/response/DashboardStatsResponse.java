package com.buurman.dto.response;

import java.math.BigDecimal;

public record DashboardStatsResponse(
    int totalProperties,
    int occupiedUnits,
    int selfOccupiedUnits,
    int vacantUnits,
    int maintenanceUnits,
    int unavailableUnits,
    int underRenovationUnits,
    int fallowUnits,
    int listedUnits,
    MonthlyIncome monthlyIncome,
    BigDecimal occupancyRate,
    BigDecimal rentalOccupancyRate) {
  public record MonthlyIncome(BigDecimal amount, String currency) {}
}
