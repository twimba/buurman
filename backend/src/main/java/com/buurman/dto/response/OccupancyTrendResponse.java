package com.buurman.dto.response;

import java.util.List;

public record OccupancyTrendResponse(
        List<DataPoint> dataPoints
) {
    public record DataPoint(
            String period,
            double occupancyRate,
            int totalUnits,
            int occupiedUnits
    ) {}
}
