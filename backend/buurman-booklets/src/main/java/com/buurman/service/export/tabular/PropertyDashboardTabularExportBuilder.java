package com.buurman.service.export.tabular;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.buurman.dto.response.PropertyDashboardResponse;
import com.buurman.dto.response.PropertyDashboardResponse.CategorySlice;
import com.buurman.dto.response.PropertyDashboardResponse.MonthlyDataPoint;
import com.buurman.dto.response.PropertyDashboardResponse.SummaryMetrics;

/** Builds the three-sheet TabularExport for a single-property dashboard. */
@Component
public class PropertyDashboardTabularExportBuilder {

  public TabularExport build(PropertyDashboardResponse dashboard) {
    SummaryMetrics s = dashboard.summary();
    TabularExport export = new TabularExport("property-dashboard");

    TabularSheet summary =
        export.addSheet(
            "Portfolio Summary Metrics",
            List.of(
                TabularColumn.text("Metric"),
                TabularColumn.of("Value", TabularColumnFormat.NUMBER)));
    summary.addRow("Currency", s.currency().orElse("N/A"));
    addNumeric(summary, "Total ROI %", s.totalRoiPercent());
    addNumeric(summary, "Annualized ROI %", s.annualizedRoiPercent());
    addNumeric(summary, "Cap Rate %", s.capRatePercent());
    addNumeric(summary, "Cash-on-Cash %", s.cashOnCashPercent());
    addNumeric(summary, "Monthly Cash Flow", s.monthlyCashFlow());
    addNumeric(summary, "Annual NOI", s.annualNoi());
    addNumeric(summary, "Total Equity", s.totalEquity());
    addNumeric(summary, "Equity Growth %", s.equityGrowthPercent());
    addNumeric(summary, "Occupancy Rate %", s.occupancyRatePercent());
    addNumeric(summary, "Gross Rent Multiplier", s.grossRentMultiplier());

    TabularSheet cashFlow =
        export.addSheet(
            "Aggregated Monthly Cash Flow",
            List.of(
                TabularColumn.text("Month"),
                TabularColumn.of("Income", TabularColumnFormat.CURRENCY),
                TabularColumn.of("Expenses", TabularColumnFormat.CURRENCY),
                TabularColumn.of("Mortgage", TabularColumnFormat.CURRENCY),
                TabularColumn.of("Net", TabularColumnFormat.CURRENCY)));
    for (MonthlyDataPoint m : dashboard.cashFlow().months()) {
      cashFlow.addRow(m.month(), m.income(), m.expenses(), m.mortgage(), m.net());
    }

    TabularSheet expense =
        export.addSheet(
            "Expense Breakdown",
            List.of(
                TabularColumn.text("Category"),
                TabularColumn.of("Amount", TabularColumnFormat.CURRENCY)));
    for (CategorySlice c : dashboard.expenseBreakdown().categories()) {
      expense.addRow(c.category(), c.amount());
    }

    return export;
  }

  private static void addNumeric(TabularSheet sheet, String label, Optional<BigDecimal> value) {
    sheet.addRow(label, value.isPresent() ? value.get() : "N/A");
  }
}
